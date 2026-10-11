package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.request.CaseEventCreateRequest;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseEventServiceTest {

    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private CaseService caseService;

    private CaseEventService caseEventService;

    @BeforeEach
    void setUp() {
        caseEventService = new CaseEventService(caseEventRepository, caseService);
    }

    @Test
    @DisplayName("일정 확정: 이벤트 저장(라벨 제목·규칙 키만·note) + stage 전이 + 갱신된 상세 반환")
    void recordRepairScheduled() {
        Case caseEntity = caseOf(CaseStage.AWAITING_RESPONSE);
        CaseOverviewResponse overview = CaseOverviewResponse.builder().caseId(6L).build();
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseService.getCaseOverview(6L, 1L)).willReturn(overview);

        CaseOverviewResponse response = caseEventService.recordEvent(6L, 1L, request(CaseEventType.REPAIR_SCHEDULED, "2026-10-01T09:00:00-04:00",
                Map.of("scheduledStart", "2026-10-08T10:00:00-04:00", "scheduledEnd", "2026-10-08T12:00:00-04:00", "unknown", "x"), "Plumber Thursday"));

        ArgumentCaptor<CaseEvent> saved = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(CaseEventType.REPAIR_SCHEDULED);
        assertThat(saved.getValue().getTitle().en()).isEqualTo("Repair Scheduled");
        assertThat(saved.getValue().getPayload()).containsOnlyKeys("scheduledStart", "scheduledEnd", "note");
        assertThat(saved.getValue().isAlert()).isFalse();
        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.REPAIR_SCHEDULED);
        assertThat(response).isSameAs(overview);
    }

    @Test
    @DisplayName("스케줄러 전용 이벤트는 EVENT_TYPE_NOT_ALLOWED")
    void serverOnlyEvent() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.AWAITING_RESPONSE));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.RESPONSE_OVERDUE, null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.EVENT_TYPE_NOT_ALLOWED);
        verify(caseEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("LOGGED에서 REPAIR_VISITED는 INVALID_TRANSITION (명세 예시)")
    void invalidTransition() {
        Case caseEntity = caseOf(CaseStage.LOGGED);
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.REPAIR_VISITED, null,
                Map.of("visitedAt", "2026-10-08T11:00:00-04:00"), null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_TRANSITION);
        assertThat(caseEntity.getStage()).isEqualTo(CaseStage.LOGGED);
    }

    @Test
    @DisplayName("311 신고에 srNumber가 없으면 INVALID_PAYLOAD")
    void missingRequiredPayload() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.FOLLOW_UP_OVERDUE));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.COMPLAINT_311_FILED, null, Map.of(), null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_PAYLOAD);
    }

    @Test
    @DisplayName("payload 시각 형식이 틀리면 INVALID_PAYLOAD")
    void invalidPayloadDateTime() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.SCHEDULING));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.REPAIR_SCHEDULED, null,
                Map.of("scheduledStart", "next Thursday"), null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_PAYLOAD);
    }

    @Test
    @DisplayName("미래 발생 시각은 INVALID_OCCURRED_AT")
    void futureOccurredAt() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.AWAITING_RESPONSE));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.LANDLORD_REFUSED,
                LocalDateTime.now().plusDays(1).toString(), null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_OCCURRED_AT);
    }

    @Test
    @DisplayName("마지막 이벤트보다 이전 시각이면 INVALID_OCCURRED_AT (진행 스텝 순서 보호)")
    void earlierThanLastEvent() {
        Case caseEntity = caseOf(CaseStage.AWAITING_RESPONSE);
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(caseEntity)).willReturn(Optional.of(
                CaseEvent.builder().caseEntity(caseEntity).type(CaseEventType.NOTICE_SENT).occurredAt(CaseEvent.parseDateTime("2026-10-10T15:00:00-04:00", ZoneId.systemDefault())).build()));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.LANDLORD_REFUSED, "2026-10-10T14:00:00-04:00", null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_OCCURRED_AT);
    }

    @Test
    @DisplayName("occurredAt 생략 시 마지막 이벤트가 현재보다 뒤면 그 시각으로 맞춘다 (순서 유지)")
    void defaultOccurredAtNotBeforeLastEvent() {
        Case caseEntity = caseOf(CaseStage.AWAITING_RESPONSE);
        LocalDateTime last = LocalDateTime.now().plusMinutes(3).withNano(0);
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(caseEntity)).willReturn(Optional.of(
                CaseEvent.builder().caseEntity(caseEntity).type(CaseEventType.NOTICE_SENT).occurredAt(last).build()));

        caseEventService.recordEvent(6L, 1L, request(CaseEventType.LANDLORD_REFUSED, null, null, null));

        ArgumentCaptor<CaseEvent> saved = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository).save(saved.capture());
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(last);
    }

    @Test
    @DisplayName("payload 값이 객체·배열이면 INVALID_PAYLOAD")
    void nonScalarPayloadValue() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.FOLLOW_UP_OVERDUE));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.COMPLAINT_311_FILED, null,
                Map.of("srNumber", Map.of("a", 1)), null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_PAYLOAD);
    }

    @Test
    @DisplayName("요청 값 오류(400)가 상태 오류(409)보다 먼저")
    void inputErrorBeforeTransitionError() {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseOf(CaseStage.LOGGED));

        assertThatThrownBy(() -> caseEventService.recordEvent(6L, 1L, request(CaseEventType.COMPLAINT_311_FILED, null, Map.of(), null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_PAYLOAD);
    }

    @Test
    @DisplayName("다음 방문 없는 방문 기록은 stage 유지, 책임전가는 alert 이벤트")
    void visitWithoutNextAndAlert() {
        Case repairDay = caseOf(CaseStage.REPAIR_DAY);
        given(caseService.findOwnedCase(6L, 1L)).willReturn(repairDay);

        caseEventService.recordEvent(6L, 1L, request(CaseEventType.REPAIR_VISITED, null, Map.of("visitedAt", "2026-10-08T11:00:00-04:00"), null));
        assertThat(repairDay.getStage()).isEqualTo(CaseStage.REPAIR_DAY);

        Case awaiting = caseOf(CaseStage.AWAITING_RESPONSE);
        given(caseService.findOwnedCase(7L, 1L)).willReturn(awaiting);
        caseEventService.recordEvent(7L, 1L, request(CaseEventType.LANDLORD_BLAME, null, null, null));

        ArgumentCaptor<CaseEvent> saved = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(1).isAlert()).isTrue();
        assertThat(awaiting.getStage()).isEqualTo(CaseStage.LANDLORD_BLAME);
    }

    // ========== 테스트 픽스처 ==========

    private Case caseOf(CaseStage stage) {
        Case caseEntity = Case.builder()
                .caseNumber("C-006")
                .title("Water Leak in Bathroom · Ceiling")
                .stage(stage)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
        return caseEntity;
    }

    private CaseEventCreateRequest request(CaseEventType type, String occurredAt, Map<String, Object> payload, String note) {
        CaseEventCreateRequest request = new CaseEventCreateRequest();
        request.setType(type);
        request.setOccurredAt(occurredAt);
        request.setPayload(payload);
        request.setNote(note);
        return request;
    }
}
