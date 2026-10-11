package com.dwell.domain.cases.service;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeType;
import com.dwell.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dwell.domain.cases.service.CaseBannerFactoryTest.event;
import static com.dwell.domain.cases.service.CaseBannerFactoryTest.newCase;
import static com.dwell.domain.cases.service.CaseBannerFactoryTest.sentNotice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseSlaServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private NoticeRepository noticeRepository;

    private CaseSlaService caseSlaService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 11, 12, 0);

    @BeforeEach
    void setUp() {
        caseSlaService = new CaseSlaService(caseRepository, caseEventRepository, noticeRepository);
        lenient().when(caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("1차 통지 기한이 지나면 RESPONSE_OVERDUE + 경고 이벤트 (발생 시각 = 기한)")
    void responseOverdue() {
        Case caseEntity = caseIn(CaseStage.AWAITING_RESPONSE);
        LocalDateTime due = NOW.minusHours(2);
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity))
                .willReturn(List.of(sentNotice(caseEntity, NoticeType.FIRST, NOW.minusHours(26), due)));

        assertThat(caseSlaService.checkCase(6L, NOW)).isTrue();

        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.RESPONSE_OVERDUE);
        CaseEvent saved = savedEvent();
        assertThat(saved.getType()).isEqualTo(CaseEventType.RESPONSE_OVERDUE);
        assertThat(saved.getOccurredAt()).isEqualTo(due);
        assertThat(saved.isAlert()).isTrue();
        assertThat(saved.getTitle().en()).isEqualTo("No Response");
    }

    @Test
    @DisplayName("기한 전이면 그대로 (이벤트 X)")
    void beforeDue() {
        Case caseEntity = caseIn(CaseStage.AWAITING_RESPONSE);
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity))
                .willReturn(List.of(sentNotice(caseEntity, NoticeType.FIRST, NOW.minusHours(2), NOW.plusMinutes(1))));

        assertThat(caseSlaService.checkCase(6L, NOW)).isFalse();

        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.AWAITING_RESPONSE);
        verify(caseEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("2차 통지 기한 경과는 FOLLOW_UP 통지 기준 → FOLLOW_UP_OVERDUE (1차 통지 기한은 무시)")
    void followUpOverdue() {
        Case caseEntity = caseIn(CaseStage.FOLLOW_UP_SENT);
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity)).willReturn(List.of(
                sentNotice(caseEntity, NoticeType.FIRST, NOW.minusDays(5), NOW.minusDays(4)),
                sentNotice(caseEntity, NoticeType.FOLLOW_UP, NOW.minusDays(3), NOW.minusHours(1))));

        assertThat(caseSlaService.checkCase(6L, NOW)).isTrue();

        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.FOLLOW_UP_OVERDUE);
        assertThat(savedEvent().getType()).isEqualTo(CaseEventType.FOLLOW_UP_OVERDUE);
    }

    @Test
    @DisplayName("기한이 마지막 이벤트보다 앞서면 발생 시각은 마지막 이벤트 시각 (타임라인 순서 유지)")
    void occurredAtNotBeforeLastEvent() {
        Case caseEntity = caseIn(CaseStage.AWAITING_RESPONSE);
        LocalDateTime lastEventAt = NOW.minusHours(1);
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity))
                .willReturn(List.of(sentNotice(caseEntity, NoticeType.FIRST, NOW.minusHours(30), NOW.minusHours(6))));
        given(caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(caseEntity))
                .willReturn(Optional.of(event(caseEntity, CaseEventType.EVIDENCE_ADDED, lastEventAt, null)));

        caseSlaService.checkCase(6L, NOW);

        assertThat(savedEvent().getOccurredAt()).isEqualTo(lastEventAt);
    }

    @Test
    @DisplayName("수리 예정일(뉴욕 날짜)이 오늘이면 REPAIR_DAY, 이벤트 없음 (실행 환경 시간대와 무관)")
    void repairDay() {
        Case caseEntity = caseIn(CaseStage.REPAIR_SCHEDULED);
        // 뉴욕 10/11 오전 9시에 체크, 수리는 같은 날 오후 2시
        LocalDateTime now = fromNewYork(LocalDateTime.of(2026, 10, 11, 9, 0));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity)).willReturn(List.of(
                event(caseEntity, CaseEventType.REPAIR_SCHEDULED, now.minusDays(2), Map.of("scheduledStart", "2026-10-11T14:00:00-04:00"))));

        assertThat(caseSlaService.checkCase(6L, now)).isTrue();

        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.REPAIR_DAY);
        verify(caseEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("수리 예정일이 내일이면 그대로")
    void repairTomorrow() {
        Case caseEntity = caseIn(CaseStage.REPAIR_SCHEDULED);
        // 뉴욕 10/11 밤 11시에 체크, 수리는 다음 날 오전 9시
        LocalDateTime now = fromNewYork(LocalDateTime.of(2026, 10, 11, 23, 0));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity)).willReturn(List.of(
                event(caseEntity, CaseEventType.REPAIR_SCHEDULED, now.minusDays(2), Map.of("scheduledStart", "2026-10-12T09:00:00-04:00"))));

        assertThat(caseSlaService.checkCase(6L, now)).isFalse();
        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.REPAIR_SCHEDULED);
    }

    @Test
    @DisplayName("대상이 아닌 stage · 없는 케이스는 아무것도 안 함")
    void notTarget() {
        Case caseEntity = caseIn(CaseStage.REPORTED_311);
        given(caseRepository.findByIdAndDeletedAtIsNull(7L)).willReturn(Optional.empty());

        assertThat(caseSlaService.checkCase(6L, NOW)).isFalse();
        assertThat(caseSlaService.checkCase(7L, NOW)).isFalse();
        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.REPORTED_311);
        verify(caseEventRepository, never()).save(any());
    }

    // ========== 테스트 픽스처 ==========

    private Case caseIn(CaseStage stage) {
        Case caseEntity = newCase(stage);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        return caseEntity;
    }

    private CaseEvent savedEvent() {
        ArgumentCaptor<CaseEvent> captor = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository).save(captor.capture());
        return captor.getValue();
    }

    // 뉴욕 현지 시각 → 저장 시간대(JVM 기본) 시각 (테스트 실행 환경 시간대와 무관하게 같은 순간)
    private LocalDateTime fromNewYork(LocalDateTime newYorkTime) {
        return newYorkTime.atZone(ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
