package com.dwell.domain.analysis.service;

import com.dwell.domain.analysis.dto.FinalAssessment;
import com.dwell.domain.analysis.dto.PolicyResult;
import com.dwell.domain.analysis.dto.VisionObservation;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.enums.Progression;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.global.ai.GeminiInspectorClient;
import com.dwell.global.ai.LlmException;
import com.dwell.global.ai.LlmMessage;
import com.dwell.global.ai.NebiusChatClient;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
케이스 심각도 분석 (F1) — 케이스 생성·증거 추가 후 비동기로 실행
1) 새 증거(analysisStatus = PENDING)의 사진·영상 -> 튜닝한 Gemini가 1개씩 1차 판정 (난방·온수는 생략)
2) 판정 + 사용자 설명 + 이전 증거 -> Nemotron(Case Agent)이 최종 판정 JSON
3) SeverityPolicy가 규칙 범위로 고침 (하향 제한, Inconclusive, 난방·온수, SLA 제안)
4) 증거별 판정 + 케이스 severity·aiAnalysis 저장, analysisStatus = DONE (예외면 FAILED)
- 모델 호출은 트랜잭션 밖에서 (DB 커넥션을 오래 잡지 않도록)
 */
@Service
@Slf4j
public class CaseAnalysisService {

    private static final Set<MediaType> VISUAL_MEDIA = Set.of(MediaType.PHOTO, MediaType.VIDEO);
    private static final int MAX_REASONS = 3;
    private static final int SUB_LOCATION_MAX_LENGTH = 20;

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;
    private final GeminiInspectorClient geminiClient;
    private final NebiusChatClient chatClient;
    private final SeverityPolicy severityPolicy;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;
    private final String judgeModel;

    public CaseAnalysisService(CaseRepository caseRepository, EvidenceRepository evidenceRepository, FileRepository fileRepository,
                               S3Uploader s3Uploader, GeminiInspectorClient geminiClient, NebiusChatClient chatClient,
                               SeverityPolicy severityPolicy, JsonMapper jsonMapper, TransactionTemplate transactionTemplate,
                               @Value("${nebius.judge-model:}") String judgeModel) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.fileRepository = fileRepository;
        this.s3Uploader = s3Uploader;
        this.geminiClient = geminiClient;
        this.chatClient = chatClient;
        this.severityPolicy = severityPolicy;
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = transactionTemplate;
        this.judgeModel = judgeModel == null ? "" : judgeModel.strip();
    }

    // 케이스 분석 실행 (실패해도 예외를 밖으로 던지지 않고 FAILED로 저장)
    public void analyze(Long caseId) {

        log.info("[CaseAnalysisService] 케이스 분석 - 시작: caseId={}", caseId);

        AnalysisInput input = transactionTemplate.execute(status -> loadInput(caseId));
        if (input == null) {
            log.warn("[CaseAnalysisService] 케이스 분석 - 케이스 없음: caseId={}", caseId);
            return;
        }

        try {
            boolean heatCase = input.issueCategory() == IssueCategory.HEAT_HOT_WATER;
            List<VisionObservation> observations = heatCase ? List.of() : inspectFiles(input.files());
            AssessedSeverity visionSeverity = heatCase ? null : aggregate(observations);
            LocalDate today = LocalDate.now();

            FinalAssessment model = judge(input, observations, visionSeverity, today);
            PolicyResult policy = severityPolicy.apply(visionSeverity, model, input.issueCategory(), input.issueDetail(), today);
            if (!policy.corrections().isEmpty()) {
                log.warn("[CaseAnalysisService] 규칙으로 모델 판정 수정: caseId={}, corrections={}", caseId, policy.corrections());
            }

            transactionTemplate.executeWithoutResult(status -> save(input, observations, model, policy));

            log.info("[CaseAnalysisService] 케이스 분석 - 완료: caseId={}, vision={}, final={}, observations={}",
                    caseId, visionSeverity, policy.finalSeverity(), observations.size());
        } catch (Exception e) {
            log.error("[CaseAnalysisService] 케이스 분석 - 실패: caseId={}", caseId, e);
            transactionTemplate.executeWithoutResult(status -> markFailed(input));
        }
    }

    // ========== 1) 입력 준비 ==========

    private AnalysisInput loadInput(Long caseId) {
        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(caseId).orElse(null);
        if (caseEntity == null) {
            return null;
        }
        List<Evidence> evidences = evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity);
        List<Evidence> pending = evidences.stream().filter(e -> e.getAnalysisStatus() == AnalysisStatus.PENDING).toList();

        // 이전에 분석한 증거 (같은 케이스 = 같은 위치) -> 악화 판단용
        List<Map<String, Object>> previous = evidences.stream()
                .filter(e -> e.getAnalysisStatus() == AnalysisStatus.DONE)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("evidence_id", e.getEvidenceCode());
                    item.put("captured_at", e.getCreatedAt() == null ? null : e.getCreatedAt().toLocalDate().toString());
                    item.put("severity", e.getSeverity() == null ? "Inconclusive" : e.getSeverity().name());
                    item.put("reason", e.getReasons() == null || e.getReasons().isEmpty() ? null : e.getReasons().getFirst().en());
                    return item;
                })
                .toList();

        List<FileInput> files = pending.isEmpty() ? List.of() : fileRepository.findAllByEvidenceInOrderByIdAsc(pending).stream()
                .filter(file -> VISUAL_MEDIA.contains(file.getMediaType()))
                .map(file -> new FileInput(file.getEvidence().getId(), file.getId(), file.getStorageKey(), file.getMimeType(),
                        file.getMediaType() == MediaType.VIDEO))
                .toList();

        // 사용자 설명: 케이스 설명 + 이번 증거 메모
        List<String> descriptions = new ArrayList<>();
        if (caseEntity.getDescription() != null) {
            descriptions.add(caseEntity.getDescription());
        }
        pending.stream().map(Evidence::getDescription)
                .filter(d -> d != null && !d.isBlank() && !d.equals(caseEntity.getDescription()))
                .forEach(descriptions::add);

        return new AnalysisInput(caseEntity.getId(), caseEntity.getCaseNumber(), caseEntity.getIssueCategory(),
                caseEntity.getIssueDetail(), caseEntity.getIssueType(),
                caseEntity.getLocation() + (caseEntity.getAffectedArea() == null ? "" : " / " + caseEntity.getAffectedArea()),
                String.join("\n", descriptions), caseEntity.getUser().getPreferredLanguage(),
                pending.stream().map(Evidence::getId).toList(), files, previous);
    }

    // ========== 2) 사진·영상 판정 (Gemini) ==========

    // 파일마다 병렬 호출 (1장에 수 초 걸림)
    private List<VisionObservation> inspectFiles(List<FileInput> files) throws Exception {
        if (files.isEmpty()) {
            return List.of();
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<VisionObservation>> futures = new ArrayList<>();
            for (FileInput file : files) {
                futures.add(executor.submit(() -> geminiClient
                        .inspect(s3Uploader.download(file.storageKey()), file.mimeType(), file.video())
                        .withIds(file.evidenceId(), file.fileId())));
            }
            List<VisionObservation> observations = new ArrayList<>();
            for (Future<VisionObservation> future : futures) {
                observations.add(future.get());
            }
            return observations;
        }
    }

    // 여러 판정 중 가장 높은 등급 (Inconclusive는 다른 판정이 하나라도 있으면 제외)
    static AssessedSeverity aggregate(List<VisionObservation> observations) {
        return observations.stream()
                .map(VisionObservation::severity)
                .filter(severity -> severity != AssessedSeverity.INCONCLUSIVE)
                .max(Comparator.comparingInt(AssessedSeverity::rank))
                .orElse(AssessedSeverity.INCONCLUSIVE);
    }

    // ========== 3) 최종 판정 (Nemotron) ==========

    // 응답 형식이 틀리면 1회 재시도, 그래도 실패하거나 키가 없으면 null (규칙이 사진 판정으로 처리)
    private FinalAssessment judge(AnalysisInput input, List<VisionObservation> observations, AssessedSeverity visionSeverity, LocalDate today) {
        if (!chatClient.isConfigured()) {
            log.warn("[CaseAnalysisService] NEBIUS_API_KEY 없음 - 사진 판정만 사용: caseId={}", input.caseId());
            return null;
        }
        List<LlmMessage> messages = List.of(
                LlmMessage.system(CaseAnalysisPrompts.judgeSystemPrompt(input.language())),
                LlmMessage.user(jsonMapper.writeValueAsString(caseContext(input, observations, visionSeverity, today))));
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String answer = judgeModel.isEmpty() ? chatClient.complete(messages) : chatClient.complete(messages, judgeModel);
                FinalAssessment assessment = parseAssessment(answer);
                if (assessment != null) {
                    return assessment;
                }
                log.warn("[CaseAnalysisService] Nemotron 응답 형식 오류: caseId={}, attempt={}", input.caseId(), attempt);
            } catch (LlmException e) {
                log.warn("[CaseAnalysisService] Nemotron 호출 실패: caseId={}, attempt={}, message={}", input.caseId(), attempt, e.getMessage());
            }
        }
        return null;
    }

    private Map<String, Object> caseContext(AnalysisInput input, List<VisionObservation> observations,
                                            AssessedSeverity visionSeverity, LocalDate today) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("case_id", input.caseNumber());
        context.put("today", today.toString());
        context.put("heat_season", SeverityPolicy.isHeatSeason(today));
        context.put("user_category", input.issueCategory().getLabel());
        context.put("user_issue_detail", input.issueDetail());
        context.put("user_location", input.location());
        context.put("user_description_original", input.description().isBlank() ? null : input.description());
        context.put("vision_severity", visionSeverity == null ? "Not_applicable" : capitalize(visionSeverity));
        context.put("observations", observations.stream().map(CaseAnalysisService::toMap).toList());
        context.put("previous_observations_same_location", input.previous());
        context.put("landlord_risk", Map.of("level", "Insufficient data"));
        return context;
    }

    // 모델 출력 -> FinalAssessment (final_severity가 없거나 틀리면 null)
    FinalAssessment parseAssessment(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        JsonNode node;
        try {
            node = jsonMapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
        AssessedSeverity finalSeverity = AssessedSeverity.parse(node.path("final_severity").asString(null));
        if (finalSeverity == null) {
            return null;
        }
        List<LocalizedText> reasons = new ArrayList<>();
        node.path("severity_reasons").forEach(reason -> {
            String en = reason.path("en").asString(null);
            if (en != null && !en.isBlank() && reasons.size() < MAX_REASONS) {
                reasons.add(new LocalizedText(en.strip(), reason.path("native").asString(null)));
            }
        });
        return new FinalAssessment(finalSeverity, blankToNull(node.path("adjustment_reason").asString(null)),
                blankToNull(node.path("adjustment_source").asString(null)),
                blankToNull(node.path("progression").asString(null)), blankToNull(node.path("progression_reason").asString(null)),
                reasons);
    }

    // ========== 4) 저장 ==========

    private void save(AnalysisInput input, List<VisionObservation> observations, FinalAssessment model, PolicyResult policy) {
        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(input.caseId()).orElse(null);
        if (caseEntity == null) {
            return; // 분석 중에 삭제됨
        }

        Progression progression = input.previous().isEmpty() || model == null ? null : toProgression(model.progression());
        LocalizedText progressionReason = progression == null || model.progressionReason() == null
                ? null : new LocalizedText(model.progressionReason(), null);

        // 증거별 판정: 그 증거 파일들의 가장 높은 판정 (악화 판단은 이번 증거에만)
        for (Evidence evidence : evidenceRepository.findAllById(input.pendingEvidenceIds())) {
            List<VisionObservation> own = observations.stream().filter(o -> evidence.getId().equals(o.evidenceId())).toList();
            VisionObservation key = keyObservation(own);
            AssessedSeverity severity = own.isEmpty() ? policy.finalSeverity() : aggregate(own);
            IssueType issueType = key == null || toIssueType(key.defectType()) == null ? input.issueType() : toIssueType(key.defectType());
            List<LocalizedText> reasons = key != null && key.defectType() != null
                    ? List.of(new LocalizedText(key.severityReason(), null))
                    : model == null ? List.of() : model.severityReasons();
            evidence.applyAnalysis(issueType, severity.toSeverity(), reasons, truncate(key == null ? null : key.location()),
                    progression, progressionReason);
        }

        caseEntity.applyAnalysis(policy.finalSeverity().toSeverity(), aiAnalysis(input, observations, model, policy, progression));
    }

    private void markFailed(AnalysisInput input) {
        caseRepository.findByIdAndDeletedAtIsNull(input.caseId()).ifPresent(Case::markAnalysisFailed);
        evidenceRepository.findAllById(input.pendingEvidenceIds()).forEach(Evidence::markAnalysisFailed);
    }

    // Case.ai_analysis (케이스 분석 화면 "Why it's high severity" + 판정 근거 감사용)
    private Map<String, Object> aiAnalysis(AnalysisInput input, List<VisionObservation> observations, FinalAssessment model,
                                           PolicyResult policy, Progression progression) {
        List<LocalizedText> reasons = model != null && !model.severityReasons().isEmpty() ? model.severityReasons()
                : observations.stream().filter(o -> o.defectType() != null)
                .map(o -> new LocalizedText(o.severityReason(), null)).distinct().limit(MAX_REASONS).toList();

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("severityReasons", reasons.stream().map(r -> {
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("en", r.en());
            text.put("native", r.nativeText());
            return text;
        }).toList());
        analysis.put("visionSeverity", policy.visionSeverity() == null ? null : policy.visionSeverity().name());
        analysis.put("finalSeverity", policy.finalSeverity().name());
        analysis.put("adjusted", policy.adjusted());
        analysis.put("adjustmentReason", model == null ? null : model.adjustmentReason());
        analysis.put("adjustmentSource", model == null ? null : model.adjustmentSource());
        analysis.put("progression", progression == null ? null : progression.name());
        analysis.put("progressionReason", progression == null || model == null ? null : model.progressionReason());
        analysis.put("typeMismatch", typeMismatch(input.issueCategory(), keyObservation(observations)));
        analysis.put("needsRetake", policy.needsRetake());
        analysis.put("proposedSlaHours", policy.proposedSlaHours());
        analysis.put("policyCorrections", policy.corrections());
        analysis.put("observations", observations.stream().map(CaseAnalysisService::toMap).toList());
        analysis.put("analyzedAt", LocalDateTime.now().toString());
        return analysis;
    }

    // ========== 헬퍼 함수 ==========

    // 가장 높은 등급의 판정 (근거 문장·위치를 대표로 씀), 판정할 수 있었던 것만
    private static VisionObservation keyObservation(List<VisionObservation> observations) {
        return observations.stream()
                .filter(o -> o.defectType() != null)
                .max(Comparator.comparingInt(o -> o.severity().rank()))
                .orElse(null);
    }

    // 사진의 하자 유형이 사용자가 고른 문제와 다르면 화면에서 확인 (누수·곰팡이 카테고리만)
    static boolean typeMismatch(IssueCategory category, VisionObservation key) {
        if (key == null || "none".equals(key.defectType())) {
            return false;
        }
        Set<String> expected = switch (category) {
            case WATER_FLOODING, PLUMBING -> Set.of("water_stain_or_leak", "both_leak_and_mold");
            case MOLD_MOISTURE -> Set.of("mold", "both_leak_and_mold", "water_stain_or_leak");
            default -> null;
        };
        return expected != null && !expected.contains(key.defectType());
    }

    private static IssueType toIssueType(String defectType) {
        if (defectType == null) {
            return null;
        }
        return switch (defectType) {
            case "water_stain_or_leak", "both_leak_and_mold" -> IssueType.WATER_LEAK;
            case "mold" -> IssueType.MOLD;
            case "crack", "other_defect" -> IssueType.OTHER;
            default -> null;
        };
    }

    private static Progression toProgression(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Progression.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // NOT_APPLICABLE 등
        }
    }

    private static Map<String, Object> toMap(VisionObservation o) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("evidence_id", o.evidenceId());
        map.put("file_id", o.fileId());
        map.put("defect_type", o.defectType());
        map.put("severity", capitalize(o.severity()));
        map.put("severity_reason", o.severityReason());
        map.put("location", o.location());
        map.put("visible_signs", o.visibleSigns());
        map.put("active_water_or_dampness", o.activeWaterOrDampness());
        map.put("extent", o.extent());
        return map;
    }

    private static String capitalize(AssessedSeverity severity) {
        String name = severity.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static String truncate(String text) {
        return text == null || text.length() <= SUB_LOCATION_MAX_LENGTH ? text : text.substring(0, SUB_LOCATION_MAX_LENGTH);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private record FileInput(Long evidenceId, Long fileId, String storageKey, String mimeType, boolean video) {
    }

    private record AnalysisInput(Long caseId, String caseNumber, IssueCategory issueCategory, String issueDetail, IssueType issueType,
                                 String location, String description, String language, List<Long> pendingEvidenceIds,
                                 List<FileInput> files, List<Map<String, Object>> previous) {
    }
}
