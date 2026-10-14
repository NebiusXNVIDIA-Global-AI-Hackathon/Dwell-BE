package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.BuildingSignal;
import com.dwell.domain.building.repository.BuildingSignalRepository;
import com.dwell.domain.cases.dto.response.CaseSummaryResponse;
import com.dwell.domain.cases.dto.response.CaseTimelineEventResponse;
import com.dwell.domain.cases.dto.response.CurrentStatusResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.BannerTone;
import com.dwell.domain.cases.enums.CaseStatus;
import com.dwell.domain.cases.enums.IssueDetail;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.service.LegalSourceService;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/*
케이스 분석 화면 (View Case Analysis)
- 심각도·근거는 CaseAnalysisService가 저장한 Case.aiAnalysis를 그대로 읽음
- Your Rights는 Legal RAG 코퍼스에서 used_by = analysis 인 근거
- 진행 판단은 배너(CaseBannerFactory)·stage 기준 규칙
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CaseSummaryService {

    static final String USAGE_ANALYSIS = "analysis";
    private static final int TIMELINE_PREVIEW_SIZE = 2;

    private final CaseService caseService;
    private final CaseEventRepository caseEventRepository;
    private final NoticeRepository noticeRepository;
    private final EvidenceRepository evidenceRepository;
    private final FileRepository fileRepository;
    private final BuildingSignalRepository buildingSignalRepository;
    private final CaseBannerFactory caseBannerFactory;
    private final CaseTimelineEventFactory caseTimelineEventFactory;
    private final LegalSourceService legalSourceService;
    private final S3Uploader s3Uploader;

    public CaseSummaryResponse getCaseSummary(Long caseId, Long userId) {

        log.info("[CaseSummaryService] 케이스 분석 조회 - 시작: caseId={}", caseId);

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseService.findOwnedCase(caseId, userId);

        // 이벤트·발송된 통지·증거·파일
        List<CaseEvent> events = caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity);
        List<Notice> sentNotices = noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity);
        List<Evidence> evidences = evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity);
        List<File> files = evidences.isEmpty() ? List.of() : fileRepository.findAllByEvidenceInOrderByIdAsc(evidences);

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> analysis = caseEntity.getAiAnalysis() == null ? Map.of() : caseEntity.getAiAnalysis();

        // 타임라인 미리보기 (최근 2건)
        List<CaseTimelineEventResponse> timeline = caseTimelineEventFactory.create(events, evidences, files, sentNotices);
        List<CaseTimelineEventResponse> timelinePreview = timeline.subList(Math.max(0, timeline.size() - TIMELINE_PREVIEW_SIZE), timeline.size());

        CaseSummaryResponse response = CaseSummaryResponse.builder()
                .caseId(caseEntity.getId())
                .caseNumber(caseEntity.getCaseNumber())
                .headline(caseEntity.getTitle())
                .address(caseService.toAddress(caseEntity))
                .analysisStatus(caseEntity.getAnalysisStatus())
                .progress(toProgress(caseEntity, caseBannerFactory.create(caseEntity, events, sentNotices, now), sentNotices, now))
                .issueDetails(CaseSummaryResponse.IssueDetails.builder()
                        .issueType(toIssueLabel(caseEntity))
                        .location(toLocationLabel(caseEntity))
                        .firstNoticedAt(caseEntity.getFirstObservedAt())
                        .reportedToLandlordAt(sentNotices.isEmpty() ? null : sentNotices.get(0).getSentAt())
                        .severity(caseEntity.getSeverity())
                        .build())
                .severityReasons(toSeverityReasons(analysis.get("severityReasons")))
                .needsRetake(Boolean.TRUE.equals(analysis.get("needsRetake")))
                .proposedSlaHours(analysis.get("proposedSlaHours") instanceof Number hours ? hours.intValue() : null)
                .buildingSignal(toBuildingSignal(caseEntity))
                .timelinePreview(timelinePreview)
                .rights(toRights(caseEntity))
                .evidence(toEvidenceThumbnails(evidences, files))
                .analysisUpdatedAt(analysis.get("analyzedAt") instanceof String at ? LocalDateTime.parse(at) : null)
                .editedByUser(false)
                .build();

        log.info("[CaseSummaryService] 케이스 분석 조회 - 종료: caseId={}, verdict={}", caseId, response.getProgress().getVerdict());

        return response;
    }

    // ========== 헬퍼 함수 ==========

    /*
    진행 판단
    - ESCALATE: 분쟁(배너 DANGER)·응답 기한 초과·재발 등 ESCALATED/DISPUTED 상태
    - NEEDS_ATTENTION: HIGH인데 아직 집주인에게 안 알림, 또는 사진 재촬영 필요
    - ON_TRACK: 그 외
    문구는 배너 제목을 그대로 씀
     */
    static CaseSummaryResponse.Progress toProgress(Case caseEntity, CurrentStatusResponse banner, List<Notice> sentNotices, LocalDateTime now) {
        CaseStatus status = caseEntity.getStage().getStatus();
        String verdict;
        if (status == CaseStatus.ESCALATED || status == CaseStatus.DISPUTED || banner.getTone() == BannerTone.DANGER
                || (banner.getDueAt() != null && banner.getDueAt().isBefore(now) && status != CaseStatus.RESOLVED)) {
            verdict = "ESCALATE";
        } else if (status != CaseStatus.RESOLVED
                && ((caseEntity.getSeverity() == Severity.HIGH && sentNotices.isEmpty()) || needsRetake(caseEntity))) {
            verdict = "NEEDS_ATTENTION";
        } else {
            verdict = "ON_TRACK";
        }
        return CaseSummaryResponse.Progress.builder()
                .verdict(verdict)
                .message(banner.getTitle())
                .build();
    }

    private static boolean needsRetake(Case caseEntity) {
        return caseEntity.getAiAnalysis() != null && Boolean.TRUE.equals(caseEntity.getAiAnalysis().get("needsRetake"));
    }

    // aiAnalysis.severityReasons [{en, native}] -> LocalizedText
    static List<LocalizedText> toSeverityReasons(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<LocalizedText> reasons = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> text && text.get("en") instanceof String en) {
                reasons.add(new LocalizedText(en, text.get("native") instanceof String nativeText ? nativeText : null));
            }
        }
        return reasons;
    }

    // 같은 건물·같은 문제 카테고리의 패턴 중 신고가 가장 많은 것
    private CaseSummaryResponse.BuildingSignal toBuildingSignal(Case caseEntity) {
        if (caseEntity.getBuilding() == null) {
            return null;
        }
        for (BuildingSignal signal : buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(caseEntity.getBuilding().getId())) {
            if (signal.getIssueCategory() == caseEntity.getIssueCategory()) {
                return CaseSummaryResponse.BuildingSignal.builder()
                        .included(false) // TODO : Add to my case analysis (PATCH /cases/{caseId}/analysis) 구현 후 저장값으로
                        .text(signal.getInsight())
                        .signalId(signal.getId())
                        .build();
            }
        }
        return null;
    }

    // Your Rights: 출처(citation)가 있는 근거만
    private List<CaseSummaryResponse.Right> toRights(Case caseEntity) {
        List<CaseSummaryResponse.Right> rights = new ArrayList<>();
        for (LegalSource source : legalSourceService.findSources(caseEntity.getIssueCategory(), USAGE_ANALYSIS)) {
            if (source.label() == null || source.label().isBlank()) {
                continue;
            }
            rights.add(CaseSummaryResponse.Right.builder()
                    .title(source.title())
                    .body(source.summary())
                    .citations(List.of(source.label()))
                    .url(source.url())
                    .build());
        }
        return rights;
    }

    private List<CaseSummaryResponse.EvidenceThumbnail> toEvidenceThumbnails(List<Evidence> evidences, List<File> files) {
        Map<Long, List<File>> filesByEvidenceId = new HashMap<>();
        for (File file : files) {
            filesByEvidenceId.computeIfAbsent(file.getEvidence().getId(), id -> new ArrayList<>()).add(file);
        }

        List<CaseSummaryResponse.EvidenceThumbnail> thumbnails = new ArrayList<>();
        for (Evidence evidence : evidences) {
            List<File> evidenceFiles = filesByEvidenceId.getOrDefault(evidence.getId(), List.of());
            File first = evidenceFiles.isEmpty() ? null : evidenceFiles.get(0);
            thumbnails.add(CaseSummaryResponse.EvidenceThumbnail.builder()
                    .evidenceId(evidence.getId())
                    .mediaType(evidence.getMediaType())
                    .thumbnailUrl(first == null ? null : s3Uploader.getPresignedUrl(first.getStorageKey()))
                    .durationSec(first == null ? null : first.getDurationSec())
                    .createdAt(evidence.getCreatedAt())
                    .build());
        }
        return thumbnails;
    }

    // 세부 이슈 문구 (Other면 직접 입력한 값)
    private static String toIssueLabel(Case caseEntity) {
        if (IssueDetail.OTHER.equals(caseEntity.getIssueDetail())) {
            return caseEntity.getIssueDetailOther();
        }
        try {
            return IssueDetail.valueOf(caseEntity.getIssueDetail()).getLabel();
        } catch (IllegalArgumentException e) {
            return caseEntity.getIssueCategory().getLabel();
        }
    }

    // "Bathroom · Ceiling" (Other면 직접 입력한 값, 부위가 없으면 공간만)
    private static String toLocationLabel(Case caseEntity) {
        String place = caseEntity.getLocation() == Location.OTHER ? caseEntity.getLocationOther() : caseEntity.getLocation().getLabel();
        if (caseEntity.getAffectedArea() == null) {
            return place;
        }
        String area = caseEntity.getAffectedArea() == AffectedArea.OTHER ? caseEntity.getAffectedAreaOther() : caseEntity.getAffectedArea().getLabel();
        return place + " · " + area;
    }
}
