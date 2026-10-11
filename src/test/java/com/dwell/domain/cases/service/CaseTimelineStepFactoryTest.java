package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.TimelineStepResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.TimelineStepState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.dwell.domain.cases.service.CaseBannerFactoryTest.event;
import static com.dwell.domain.cases.service.CaseBannerFactoryTest.newCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CaseTimelineStepFactoryTest {

    // 테스트 시각은 UTC로 저장됐다고 가정
    private final CaseTimelineStepFactory factory = new CaseTimelineStepFactory(ZoneOffset.UTC);

    // 뉴욕 9/28 저녁 = UTC 9/29 새벽
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 9, 29, 0, 12);
    private static final LocalDateTime NOTIFIED = LocalDateTime.of(2026, 9, 29, 0, 30);

    @ParameterizedTest
    @EnumSource(CaseStage.class)
    @DisplayName("19개 stage 모두 현재 스텝이 정확히 1개, 최대 4개")
    void allStagesHaveOneCurrentStep(CaseStage stage) {
        List<TimelineStepResponse> steps = factory.create(newCase(stage), List.of());

        assertThat(steps).hasSizeLessThanOrEqualTo(4);
        assertThat(steps).filteredOn(step -> step.getState() == TimelineStepState.CURRENT || step.getState() == TimelineStepState.ALERT)
                .hasSize(1);
    }

    @Test
    @DisplayName("LOGGED: Case Created → Notified Landlord(현재) → Waiting(다음), 날짜는 뉴욕 기준")
    void logged() {
        Case caseEntity = newCase(CaseStage.LOGGED);
        List<CaseEvent> events = List.of(event(caseEntity, CaseEventType.CASE_CREATED, CREATED, null));

        List<TimelineStepResponse> steps = factory.create(caseEntity, events);

        assertThat(steps).extracting("label", "date", "state").containsExactly(
                tuple("Case Created", LocalDate.of(2026, 9, 28), TimelineStepState.DONE),
                tuple("Notified Landlord", null, TimelineStepState.CURRENT),
                tuple("Waiting for Landlord's Response", null, TimelineStepState.TODO));
    }

    @Test
    @DisplayName("AWAITING_RESPONSE: 와프와 동일한 4스텝")
    void awaitingResponse() {
        Case caseEntity = newCase(CaseStage.AWAITING_RESPONSE);
        List<CaseEvent> events = List.of(
                event(caseEntity, CaseEventType.CASE_CREATED, CREATED, null),
                event(caseEntity, CaseEventType.EVIDENCE_ADDED, CREATED, null),
                event(caseEntity, CaseEventType.NOTICE_SENT, NOTIFIED, null));

        List<TimelineStepResponse> steps = factory.create(caseEntity, events);

        assertThat(steps).extracting("label", "state").containsExactly(
                tuple("Case Created", TimelineStepState.DONE),
                tuple("Notified Landlord", TimelineStepState.DONE),
                tuple("Waiting for Landlord's Response", TimelineStepState.CURRENT),
                tuple("Follow Up", TimelineStepState.TODO));
    }

    @Test
    @DisplayName("RESPONSE_OVERDUE: 기한 경과 이벤트가 빨간 현재 스텝이 되고 날짜를 가진다")
    void responseOverdue() {
        Case caseEntity = newCase(CaseStage.RESPONSE_OVERDUE);
        List<CaseEvent> events = List.of(
                event(caseEntity, CaseEventType.CASE_CREATED, CREATED, null),
                event(caseEntity, CaseEventType.NOTICE_SENT, NOTIFIED, null),
                event(caseEntity, CaseEventType.RESPONSE_OVERDUE, LocalDateTime.of(2026, 9, 30, 0, 30), null));

        List<TimelineStepResponse> steps = factory.create(caseEntity, events);

        assertThat(steps).extracting("label", "date", "state").containsExactly(
                tuple("Case Created", LocalDate.of(2026, 9, 28), TimelineStepState.DONE),
                tuple("Notified Landlord", LocalDate.of(2026, 9, 28), TimelineStepState.DONE),
                tuple("Response Overdue", LocalDate.of(2026, 9, 29), TimelineStepState.ALERT),
                tuple("Follow Up", null, TimelineStepState.TODO));
    }

    @Test
    @DisplayName("REPAIR_IN_PROGRESS: 방문 순번 라벨 + 다음 방문일이 현재 스텝 날짜")
    void repairInProgress() {
        Case caseEntity = newCase(CaseStage.REPAIR_IN_PROGRESS);
        List<CaseEvent> events = List.of(
                event(caseEntity, CaseEventType.REPAIR_SCHEDULED, LocalDateTime.of(2026, 10, 1, 13, 0), null),
                event(caseEntity, CaseEventType.REPAIR_VISITED, LocalDateTime.of(2026, 10, 8, 16, 0),
                        Map.of("visitedAt", "2026-10-08T11:00:00-04:00", "nextVisitAt", "2026-10-12T10:00:00-04:00")));

        List<TimelineStepResponse> steps = factory.create(caseEntity, events);

        assertThat(steps).extracting("label", "date", "state").containsExactly(
                tuple("Repair Scheduled", LocalDate.of(2026, 10, 1), TimelineStepState.DONE),
                tuple("1st Visit", LocalDate.of(2026, 10, 8), TimelineStepState.DONE),
                tuple("Next Visit", LocalDate.of(2026, 10, 12), TimelineStepState.CURRENT));
    }

    @Test
    @DisplayName("지난 스텝이 많으면 최근 것만 남겨 최대 4개")
    void keepsLatestPastSteps() {
        Case caseEntity = newCase(CaseStage.RESOLVED);
        List<CaseEvent> events = List.of(
                event(caseEntity, CaseEventType.CASE_CREATED, CREATED, null),
                event(caseEntity, CaseEventType.NOTICE_SENT, NOTIFIED, null),
                event(caseEntity, CaseEventType.LANDLORD_REPLIED, LocalDateTime.of(2026, 9, 29, 17, 0), null),
                event(caseEntity, CaseEventType.REPAIR_SCHEDULED, LocalDateTime.of(2026, 10, 1, 13, 0), null),
                event(caseEntity, CaseEventType.REPAIR_VERIFIED, LocalDateTime.of(2026, 10, 15, 13, 0), null),
                event(caseEntity, CaseEventType.RESOLVED, LocalDateTime.of(2026, 10, 15, 14, 0), null));

        List<TimelineStepResponse> steps = factory.create(caseEntity, events);

        assertThat(steps).extracting("label").containsExactly("Landlord Replied", "Repair Scheduled", "Repaired", "Resolved");
        assertThat(steps.get(3).getState()).isEqualTo(TimelineStepState.CURRENT);
    }
}
