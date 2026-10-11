package com.dwell.domain.cases.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseEventRuleTest {

    @ParameterizedTest
    @EnumSource(value = CaseEventType.class, names = {"CASE_CREATED", "EVIDENCE_ADDED", "NOTICE_SENT", "FOLLOW_UP_SENT",
            "RESPONSE_OVERDUE", "FOLLOW_UP_OVERDUE", "SCHEDULING", "REPAIR_VERIFIED", "RESOLVED"})
    @DisplayName("서버 전용·구현 대기 이벤트는 규칙이 없다 (클라 기록 불가)")
    void serverOnlyEventsHaveNoRule(CaseEventType type) {
        assertThat(CaseEventRule.of(type)).isEmpty();
    }

    @Test
    @DisplayName("LOGGED에서는 어떤 이벤트도 기록할 수 없다 (통지 발송 API로만 시작)")
    void nothingFromLogged() {
        for (CaseEventRule rule : CaseEventRule.values()) {
            assertThat(rule.canTransitFrom(CaseStage.LOGGED)).as(rule.name()).isFalse();
        }
    }

    @Test
    @DisplayName("RESOLVED에서는 RECURRED만 가능")
    void onlyRecurredFromResolved() {
        for (CaseEventRule rule : CaseEventRule.values()) {
            assertThat(rule.canTransitFrom(CaseStage.RESOLVED)).as(rule.name()).isEqualTo(rule == CaseEventRule.RECURRED);
        }
    }

    @Test
    @DisplayName("와프 Lane 1·3: 답장 대기에서 일정 확정 / 모호한 답장 / 책임전가")
    void fromAwaitingResponse() {
        assertThat(CaseEventRule.REPAIR_SCHEDULED.canTransitFrom(CaseStage.AWAITING_RESPONSE)).isTrue();
        assertThat(CaseEventRule.LANDLORD_REPLIED.canTransitFrom(CaseStage.AWAITING_RESPONSE)).isTrue();
        assertThat(CaseEventRule.LANDLORD_BLAME.canTransitFrom(CaseStage.AWAITING_RESPONSE)).isTrue();
        assertThat(CaseEventRule.LANDLORD_REPLIED.getTargetStage()).isEqualTo(CaseStage.SCHEDULING);
    }

    @Test
    @DisplayName("와프 Lane 2: 311 → HPD 점검 → 위반 순서 (311에서 바로 위반 X)")
    void publicReportOrder() {
        assertThat(CaseEventRule.COMPLAINT_311_FILED.canTransitFrom(CaseStage.FOLLOW_UP_OVERDUE)).isTrue();
        assertThat(CaseEventRule.HPD_INSPECTION_SCHEDULED.canTransitFrom(CaseStage.REPORTED_311)).isTrue();
        assertThat(CaseEventRule.VIOLATION_ISSUED.canTransitFrom(CaseStage.HPD_INSPECTION)).isTrue();
        assertThat(CaseEventRule.VIOLATION_ISSUED.canTransitFrom(CaseStage.REPORTED_311)).isFalse();
    }

    @Test
    @DisplayName("와프 Lane 4: 자가 수리 → 청구 → 보험, 자가 수리에서 보험 직행도 가능")
    void selfRepairFlow() {
        assertThat(CaseEventRule.CLAIM_SENT.canTransitFrom(CaseStage.SELF_REPAIR)).isTrue();
        assertThat(CaseEventRule.INSURANCE_CLAIM_FILED.canTransitFrom(CaseStage.CLAIM_SENT)).isTrue();
        assertThat(CaseEventRule.INSURANCE_CLAIM_FILED.canTransitFrom(CaseStage.SELF_REPAIR)).isTrue();
    }

    @Test
    @DisplayName("REPAIR_VISITED: 다음 방문이 있으면 IN_PROGRESS, 없으면 현재 stage 유지")
    void repairVisitedNextStage() {
        assertThat(CaseEventRule.REPAIR_VISITED.nextStage(CaseStage.REPAIR_DAY, Map.of("visitedAt", "2026-10-08T11:00:00-04:00", "nextVisitAt", "2026-10-12T10:00:00-04:00")))
                .isEqualTo(CaseStage.REPAIR_IN_PROGRESS);
        assertThat(CaseEventRule.REPAIR_VISITED.nextStage(CaseStage.REPAIR_DAY, Map.of("visitedAt", "2026-10-08T11:00:00-04:00")))
                .isEqualTo(CaseStage.REPAIR_DAY);
    }

    @Test
    @DisplayName("거절·책임전가·위반은 진행 스텝 빨간 표시(alert)")
    void alertEvents() {
        assertThat(CaseEventRule.LANDLORD_REFUSED.isAlert()).isTrue();
        assertThat(CaseEventRule.LANDLORD_BLAME.isAlert()).isTrue();
        assertThat(CaseEventRule.VIOLATION_ISSUED.isAlert()).isTrue();
        assertThat(CaseEventRule.REPAIR_SCHEDULED.isAlert()).isFalse();
    }
}
