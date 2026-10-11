package com.dwell.domain.cases.entity;

import com.dwell.domain.cases.enums.CaseEventRule;
import com.dwell.domain.cases.enums.CaseEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseEventShiftTest {

    @Test
    @DisplayName("데모 시간 이동: 발생 시각과 payload 시각을 과거로, 형식 유지, 시각이 아닌 값은 그대로")
    void shiftTimes() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scheduledStart", "2026-10-14T10:00:00-04:00");
        payload.put("scheduledEnd", "2026-10-14T12:00:00");
        payload.put("repairDate", "2026-10-14");
        payload.put("repairedBy", "ABC Plumbing");
        CaseEvent event = CaseEvent.builder()
                .type(CaseEventType.REPAIR_SCHEDULED)
                .occurredAt(LocalDateTime.of(2026, 10, 11, 9, 0))
                .payload(payload)
                .build();

        event.shiftTimes(CaseEventRule.DATE_TIME_KEYS, 24);

        assertThat(event.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 9, 0));
        assertThat(event.getPayload())
                .containsEntry("scheduledStart", "2026-10-13T10:00-04:00")
                .containsEntry("scheduledEnd", "2026-10-13T12:00")
                .containsEntry("repairDate", "2026-10-13")
                .containsEntry("repairedBy", "ABC Plumbing");
    }

    @Test
    @DisplayName("날짜만 있는 값은 12시간 이하 이동이면 날짜 유지")
    void dateOnlyKeepsDateForShortShift() {
        CaseEvent event = CaseEvent.builder()
                .type(CaseEventType.RECURRED)
                .occurredAt(LocalDateTime.of(2026, 10, 11, 9, 0))
                .payload(Map.of("recurredAt", "2026-10-14"))
                .build();

        event.shiftTimes(CaseEventRule.DATE_TIME_KEYS, 6);

        assertThat(event.getPayload()).containsEntry("recurredAt", "2026-10-14");
    }
}
