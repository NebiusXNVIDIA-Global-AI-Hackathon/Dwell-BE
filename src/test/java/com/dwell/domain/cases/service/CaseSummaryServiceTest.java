package com.dwell.domain.cases.service;

import com.dwell.domain.building.repository.BuildingSignalRepository;
import com.dwell.domain.cases.dto.response.CaseSummaryResponse;
import com.dwell.domain.cases.dto.response.CaseTimelineEventResponse;
import com.dwell.domain.cases.dto.response.CurrentStatusResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.BannerTone;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.service.LegalSourceService;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CaseSummaryServiceTest {

    @Mock
    private CaseService caseService;
    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private BuildingSignalRepository buildingSignalRepository;
    @Mock
    private CaseBannerFactory caseBannerFactory;
    @Mock
    private CaseTimelineEventFactory caseTimelineEventFactory;
    @Mock
    private LegalSourceService legalSourceService;
    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private CaseSummaryService caseSummaryService;

    private Case caseEntity;

    @BeforeEach
    void setUp() {
        caseEntity = Case.builder()
                .caseNumber("C-006")
                .user(mock(User.class))
                .title("Water Leak in Bathroom · Ceiling")
                .issueCategory(IssueCategory.WATER_FLOODING)
                .issueDetail("WATER_LEAK")
                .issueType(IssueType.WATER_LEAK)
                .location(Location.BATHROOM)
                .affectedArea(AffectedArea.CEILING)
                .stage(CaseStage.LOGGED)
                .analysisStatus(AnalysisStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
    }

    @Test
    @DisplayName("분석 결과(aiAnalysis)·Legal RAG·타임라인 최근 2건으로 케이스 분석 응답")
    void getCaseSummary() {
        caseEntity.applyAnalysis(Severity.HIGH, Map.of(
                "severityReasons", List.of(Map.of("en", "Water is actively dripping", "native", "물이 떨어지고 있어요")),
                "needsRetake", false,
                "proposedSlaHours", 24,
                "analyzedAt", "2026-10-14T10:00:00"));
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity)).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity)).willReturn(List.of());
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity)).willReturn(List.of());
        given(caseBannerFactory.create(any(), any(), any(), any())).willReturn(banner(BannerTone.DEFAULT, null));
        given(caseTimelineEventFactory.create(any(), any(), any(), any())).willReturn(List.of(
                event(1L), event(2L), event(3L)));
        given(legalSourceService.findSources(IssueCategory.WATER_FLOODING, "analysis")).willReturn(List.of(
                new LegalSource("NYC-HMC-27-2005", "NYC Admin. Code § 27-2005", "Duties of owner", "https://nyc.gov", "Keep in good repair."),
                new LegalSource("NO-CITATION", null, "No citation", null, "x")));

        CaseSummaryResponse response = caseSummaryService.getCaseSummary(6L, 1L);

        assertThat(response.getIssueDetails().getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(response.getIssueDetails().getLocation()).isEqualTo("Bathroom · Ceiling");
        assertThat(response.getSeverityReasons()).containsExactly(new LocalizedText("Water is actively dripping", "물이 떨어지고 있어요"));
        assertThat(response.getProposedSlaHours()).isEqualTo(24);
        assertThat(response.getAnalysisUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-10-14T10:00:00"));
        assertThat(response.getTimelinePreview()).extracting(CaseTimelineEventResponse::getEventId).containsExactly(2L, 3L);
        assertThat(response.getRights()).hasSize(1);
        assertThat(response.getRights().getFirst().getCitations()).containsExactly("NYC Admin. Code § 27-2005");
        assertThat(response.getBuildingSignal()).isNull(); // 건물 없음
        // HIGH인데 아직 집주인에게 안 알림
        assertThat(response.getProgress().getVerdict()).isEqualTo("NEEDS_ATTENTION");
    }

    @Test
    @DisplayName("진행 판단: 분쟁·기한 초과면 ESCALATE, 알렸고 기한 안이면 ON_TRACK")
    void progressVerdict() {
        LocalDateTime now = LocalDateTime.parse("2026-10-14T12:00:00");
        List<Notice> sent = List.of(mock(Notice.class));

        caseEntity.changeStage(CaseStage.AWAITING_RESPONSE);
        assertThat(CaseSummaryService.toProgress(caseEntity, banner(BannerTone.DEFAULT, now.plusHours(3)), sent, now).getVerdict())
                .isEqualTo("ON_TRACK");
        assertThat(CaseSummaryService.toProgress(caseEntity, banner(BannerTone.DEFAULT, now.minusHours(1)), sent, now).getVerdict())
                .isEqualTo("ESCALATE");

        caseEntity.changeStage(CaseStage.LANDLORD_REFUSED);
        assertThat(CaseSummaryService.toProgress(caseEntity, banner(BannerTone.DANGER, null), sent, now).getVerdict())
                .isEqualTo("ESCALATE");
    }

    @Test
    @DisplayName("severityReasons 파싱: 형식이 틀린 항목은 건너뜀")
    void toSeverityReasons() {
        assertThat(CaseSummaryService.toSeverityReasons(null)).isEmpty();
        assertThat(CaseSummaryService.toSeverityReasons(List.of(Map.of("en", "a"), Map.of("native", "b"), "c")))
                .containsExactly(new LocalizedText("a", null));
    }

    private static CurrentStatusResponse banner(BannerTone tone, LocalDateTime dueAt) {
        return CurrentStatusResponse.builder().title(new LocalizedText("Waiting for landlord", null)).tone(tone).dueAt(dueAt).build();
    }

    private static CaseTimelineEventResponse event(Long id) {
        return CaseTimelineEventResponse.builder().eventId(id).build();
    }
}
