package com.dwell.domain.analysis.service;

import com.dwell.domain.analysis.dto.VisionObservation;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.user.entity.User;
import com.dwell.global.ai.GeminiInspectorClient;
import com.dwell.global.ai.NebiusChatClient;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.common.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseAnalysisServiceTest {

    private static final String JUDGE_HIGH = """
            {"final_severity":"High","adjustment_reason":"User reports active dripping since yesterday.",
             "adjustment_source":"user_description","progression":"Not_applicable","progression_reason":null,
             "severity_reasons":[{"en":"Water is actively dripping, not just a dry stain","native":"마른 얼룩이 아니라 물이 떨어지고 있어요"},
                                 {"en":"Ongoing moisture can lead to mold","native":"습기가 계속되면 곰팡이가 생길 수 있어요"}]}""";

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private S3Uploader s3Uploader;
    @Mock
    private GeminiInspectorClient geminiClient;
    @Mock
    private NebiusChatClient chatClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    private CaseAnalysisService service;
    private Case caseEntity;
    private Evidence evidence;

    @BeforeEach
    void setUp() {
        service = new CaseAnalysisService(caseRepository, evidenceRepository, fileRepository, s3Uploader, geminiClient, chatClient,
                new SeverityPolicy(), JsonMapper.builder().build(), transactionTemplate, "");

        // 트랜잭션은 바로 실행
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
        lenient().doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 데모: 이지원 화장실 천장 누수, 설명 "어제부터 물이 떨어져요"
        User user = mock(User.class);
        lenient().when(user.getPreferredLanguage()).thenReturn("ko");
        caseEntity = Case.builder()
                .caseNumber("C-006")
                .user(user)
                .issueCategory(IssueCategory.WATER_FLOODING)
                .issueDetail("WATER_LEAK")
                .issueType(IssueType.WATER_LEAK)
                .location(Location.BATHROOM)
                .affectedArea(AffectedArea.CEILING)
                .description("어제부터 화장실 천장에서 물이 떨어져요")
                .analysisStatus(AnalysisStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);

        evidence = Evidence.builder().caseEntity(caseEntity).user(user).evidenceCode("EV-031").mediaType(MediaType.PHOTO)
                .location(Location.BATHROOM).analysisStatus(AnalysisStatus.PENDING).build();
        ReflectionTestUtils.setField(evidence, "id", 31L);

        File photo = File.builder().publicId("f_1").user(user).mediaType(MediaType.PHOTO).mimeType("image/jpeg")
                .storageKey("evidence/k1").sha256("x").build();
        ReflectionTestUtils.setField(photo, "id", 101L);
        photo.linkEvidence(evidence);

        lenient().when(caseRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(caseEntity));
        lenient().when(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity)).thenReturn(List.of(evidence));
        lenient().when(evidenceRepository.findAllById(List.of(31L))).thenReturn(List.of(evidence));
        lenient().when(fileRepository.findAllByEvidenceInOrderByIdAsc(List.of(evidence))).thenReturn(List.of(photo));
        lenient().when(s3Uploader.download("evidence/k1")).thenReturn(new byte[]{1, 2, 3});
        lenient().when(geminiClient.inspect(any(), anyString(), anyBoolean())).thenReturn(new VisionObservation(null, null,
                "water_stain_or_leak", AssessedSeverity.MEDIUM, "A large dry brown ring stain; no active dripping visible.",
                "ceiling", List.of("brown ring stain"), "no", "medium"));
    }

    @Test
    @DisplayName("데모: 사진 Medium -> 설명으로 Nemotron이 High 상향 -> 케이스 HIGH, 증거는 사진 판정 MEDIUM")
    void demoScenario() {
        given(chatClient.isConfigured()).willReturn(true);
        given(chatClient.complete(any())).willReturn(JUDGE_HIGH);

        service.analyze(6L);

        assertThat(caseEntity.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(caseEntity.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        Map<String, Object> analysis = caseEntity.getAiAnalysis();
        assertThat(analysis).containsEntry("visionSeverity", "MEDIUM").containsEntry("finalSeverity", "HIGH")
                .containsEntry("adjusted", true).containsEntry("adjustmentSource", "user_description")
                .containsEntry("proposedSlaHours", 24).containsEntry("needsRetake", false).containsEntry("typeMismatch", false);
        assertThat((List<?>) analysis.get("severityReasons")).hasSize(2);

        assertThat(evidence.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(evidence.getIssueType()).isEqualTo(IssueType.WATER_LEAK);
        assertThat(evidence.getSubLocation()).isEqualTo("ceiling");
        assertThat(evidence.getReasons().getFirst().en()).contains("dry brown ring stain");
        assertThat(evidence.getProgression()).isNull(); // 이전 증거 없음
        assertThat(evidence.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    @Test
    @DisplayName("NEBIUS_API_KEY가 없으면 사진 판정 그대로 (MEDIUM, SLA 72h)")
    void withoutNemotron() {
        given(chatClient.isConfigured()).willReturn(false);

        service.analyze(6L);

        assertThat(caseEntity.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(caseEntity.getAiAnalysis()).containsEntry("proposedSlaHours", 72).containsEntry("adjusted", false);
        assertThat((List<?>) caseEntity.getAiAnalysis().get("severityReasons")).hasSize(1);
    }

    @Test
    @DisplayName("Nemotron 응답 형식이 두 번 틀리면 사진 판정으로")
    void invalidJudgeAnswer() {
        given(chatClient.isConfigured()).willReturn(true);
        given(chatClient.complete(any())).willReturn("I think it's bad.");

        service.analyze(6L);

        verify(chatClient, times(2)).complete(any());
        assertThat(caseEntity.getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    @DisplayName("파일을 못 읽는 등 예외가 나면 케이스·증거 FAILED")
    void failure() {
        willAnswer(invocation -> {
            throw new IllegalStateException("S3 down");
        }).given(s3Uploader).download("evidence/k1");

        service.analyze(6L);

        assertThat(caseEntity.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(evidence.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(caseEntity.getSeverity()).isNull();
    }

    @Test
    @DisplayName("여러 판정 합치기: 가장 높은 등급, Inconclusive는 다른 판정이 있으면 제외")
    void aggregate() {
        assertThat(CaseAnalysisService.aggregate(List.of(observation(AssessedSeverity.LOW), observation(AssessedSeverity.INCONCLUSIVE),
                observation(AssessedSeverity.HIGH)))).isEqualTo(AssessedSeverity.HIGH);
        assertThat(CaseAnalysisService.aggregate(List.of(observation(AssessedSeverity.INCONCLUSIVE)))).isEqualTo(AssessedSeverity.INCONCLUSIVE);
        assertThat(CaseAnalysisService.aggregate(List.of())).isEqualTo(AssessedSeverity.INCONCLUSIVE);
    }

    @Test
    @DisplayName("사진 하자 유형이 사용자가 고른 문제와 다르면 typeMismatch")
    void typeMismatch() {
        assertThat(CaseAnalysisService.typeMismatch(IssueCategory.WATER_FLOODING, observation("mold"))).isTrue();
        assertThat(CaseAnalysisService.typeMismatch(IssueCategory.WATER_FLOODING, observation("water_stain_or_leak"))).isFalse();
        assertThat(CaseAnalysisService.typeMismatch(IssueCategory.MOLD_MOISTURE, observation("crack"))).isTrue();
        assertThat(CaseAnalysisService.typeMismatch(IssueCategory.WATER_FLOODING, observation("none"))).isFalse();
        assertThat(CaseAnalysisService.typeMismatch(IssueCategory.PESTS, observation("mold"))).isFalse();
    }

    @Test
    @DisplayName("Nemotron 출력 파싱 (코드블록에 감싸져도, 근거는 최대 3개)")
    void parseAssessment() {
        var assessment = service.parseAssessment("```json\n" + JUDGE_HIGH + "\n```");

        assertThat(assessment.finalSeverity()).isEqualTo(AssessedSeverity.HIGH);
        assertThat(assessment.severityReasons().getFirst().nativeText()).startsWith("마른 얼룩");
        assertThat(service.parseAssessment("{\"final_severity\":\"Critical\"}")).isNull();
    }

    private static VisionObservation observation(AssessedSeverity severity) {
        return new VisionObservation(1L, 1L, "water_stain_or_leak", severity, "r", null, List.of(), null, null);
    }

    private static VisionObservation observation(String defectType) {
        return new VisionObservation(1L, 1L, defectType, AssessedSeverity.LOW, "r", null, List.of(), null, null);
    }
}
