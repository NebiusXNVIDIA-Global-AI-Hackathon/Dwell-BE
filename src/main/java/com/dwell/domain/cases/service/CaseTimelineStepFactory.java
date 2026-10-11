package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.TimelineStepResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.TimelineStepState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/*
Case Detail 진행 스텝 생성 (와프 찐최종 기준)
지나간 이벤트 최근 N개(DONE / ALERT) + 현재 단계(CURRENT / ALERT) + 예상 다음 단계(TODO), 최대 4개
 */
@Component
public class CaseTimelineStepFactory {

    private static final int MAX_STEPS = 4;

    // 스텝 날짜는 뉴욕 날짜 기준
    private static final ZoneId CASE_ZONE = ZoneId.of("America/New_York");

    // DB의 LocalDateTime은 JVM 기본 시간대로 저장됨 (DwellApplication에서 America/New_York으로 고정)
    private final ZoneId storageZone;

    public CaseTimelineStepFactory() {
        this(ZoneId.systemDefault());
    }

    // 테스트에서 시간대 고정용
    CaseTimelineStepFactory(ZoneId storageZone) {
        this.storageZone = storageZone;
    }

    // 이벤트 → 스텝 라벨 (EVIDENCE_ADDED, SCHEDULING은 스텝으로 안 보여줌)
    private static final Map<CaseEventType, String> EVENT_LABELS = new EnumMap<>(CaseEventType.class);

    // stage → 현재/다음 스텝 정의
    private static final Map<CaseStage, StageStep> STAGE_STEPS = new EnumMap<>(CaseStage.class);

    static {
        for (CaseEventType type : CaseEventType.values()) {
            if (type != CaseEventType.EVIDENCE_ADDED && type != CaseEventType.SCHEDULING) {
                EVENT_LABELS.put(type, type.getLabel());
            }
        }

        // entryEvent가 null이면 '대기' 단계 → 날짜 없는 현재 스텝을 새로 만듦
        // entryEvent가 있으면 그 이벤트 자체가 현재 스텝이 됨 (날짜 = 이벤트 날짜)
        STAGE_STEPS.put(CaseStage.LOGGED, new StageStep("Notified Landlord", TimelineStepState.CURRENT, null, "Waiting for Landlord's Response"));
        STAGE_STEPS.put(CaseStage.AWAITING_RESPONSE, new StageStep("Waiting for Landlord's Response", TimelineStepState.CURRENT, null, "Follow Up"));
        STAGE_STEPS.put(CaseStage.RESPONSE_OVERDUE, new StageStep("Response Overdue", TimelineStepState.ALERT, CaseEventType.RESPONSE_OVERDUE, "Follow Up"));
        STAGE_STEPS.put(CaseStage.FOLLOW_UP_SENT, new StageStep("Waiting for Landlord's Response", TimelineStepState.CURRENT, null, null));
        STAGE_STEPS.put(CaseStage.FOLLOW_UP_OVERDUE, new StageStep("Still no Response", TimelineStepState.ALERT, CaseEventType.FOLLOW_UP_OVERDUE, "Filed 311"));
        STAGE_STEPS.put(CaseStage.SCHEDULING, new StageStep("Scheduling", TimelineStepState.CURRENT, null, null));
        STAGE_STEPS.put(CaseStage.REPAIR_SCHEDULED, new StageStep("Waiting for Repair", TimelineStepState.CURRENT, null, null));
        STAGE_STEPS.put(CaseStage.REPAIR_DAY, new StageStep("Repair Day", TimelineStepState.CURRENT, null, null));
        STAGE_STEPS.put(CaseStage.REPAIR_IN_PROGRESS, new StageStep("Next Visit", TimelineStepState.CURRENT, null, null));
        STAGE_STEPS.put(CaseStage.LANDLORD_REFUSED, new StageStep("Landlord Refused", TimelineStepState.ALERT, CaseEventType.LANDLORD_REFUSED, "Filed 311"));
        STAGE_STEPS.put(CaseStage.LANDLORD_BLAME, new StageStep("Landlord Blame Shifting", TimelineStepState.ALERT, CaseEventType.LANDLORD_BLAME, "Filed 311"));
        STAGE_STEPS.put(CaseStage.REPORTED_311, new StageStep("311 Filed", TimelineStepState.CURRENT, CaseEventType.COMPLAINT_311_FILED, "HPD Inspection"));
        STAGE_STEPS.put(CaseStage.HPD_INSPECTION, new StageStep("HPD Inspection", TimelineStepState.CURRENT, CaseEventType.HPD_INSPECTION_SCHEDULED, "Notice of Violation"));
        STAGE_STEPS.put(CaseStage.VIOLATION_UNRESOLVED, new StageStep("Violation Issued", TimelineStepState.ALERT, CaseEventType.VIOLATION_ISSUED, null));
        STAGE_STEPS.put(CaseStage.SELF_REPAIR, new StageStep("Repairing Myself", TimelineStepState.CURRENT, CaseEventType.SELF_REPAIR_STARTED, null));
        STAGE_STEPS.put(CaseStage.CLAIM_SENT, new StageStep("Claim Sent", TimelineStepState.CURRENT, CaseEventType.CLAIM_SENT, null));
        STAGE_STEPS.put(CaseStage.INSURANCE_CLAIM, new StageStep("Claim Filed", TimelineStepState.CURRENT, CaseEventType.INSURANCE_CLAIM_FILED, null));
        STAGE_STEPS.put(CaseStage.RESOLVED, new StageStep("Resolved", TimelineStepState.CURRENT, CaseEventType.RESOLVED, null));
        STAGE_STEPS.put(CaseStage.RECURRED, new StageStep("Recurred", TimelineStepState.CURRENT, CaseEventType.RECURRED, null));
    }

    public List<TimelineStepResponse> create(Case caseEntity, List<CaseEvent> events) {

        StageStep stageStep = STAGE_STEPS.get(caseEntity.getStage());

        // 1. 현재 단계를 나타내는 이벤트 찾기 (대기 단계면 null)
        CaseEvent entryEvent = null;
        if (stageStep.entryEvent() != null) {
            entryEvent = findLatestEvent(events, stageStep.entryEvent());
        }

        // 2. 지나간 스텝 (현재 단계 이벤트는 제외)
        List<TimelineStepResponse> pastSteps = new ArrayList<>();
        int visitCount = 0;
        for (CaseEvent event : events) {
            if (event == entryEvent || !EVENT_LABELS.containsKey(event.getType())) {
                continue;
            }

            String label = EVENT_LABELS.get(event.getType());
            if (event.getType() == CaseEventType.REPAIR_VISITED) {
                visitCount++;
                label = toOrdinal(visitCount) + " Visit";
            }

            pastSteps.add(toStep(label, stepDate(event), event.isAlert() ? TimelineStepState.ALERT : TimelineStepState.DONE));
        }

        // 3. 현재 스텝
        TimelineStepResponse currentStep = toStep(stageStep.currentLabel(), currentDate(caseEntity.getStage(), entryEvent, events), stageStep.currentState());

        // 4. 최대 4개가 되도록 지나간 스텝은 최근 것만
        int pastLimit = MAX_STEPS - 1 - (stageStep.nextLabel() != null ? 1 : 0);
        List<TimelineStepResponse> steps = new ArrayList<>(pastSteps.subList(Math.max(0, pastSteps.size() - pastLimit), pastSteps.size()));
        steps.add(currentStep);

        // 5. 예상 다음 스텝 (흐리게 표시)
        if (stageStep.nextLabel() != null) {
            steps.add(toStep(stageStep.nextLabel(), null, TimelineStepState.TODO));
        }

        return steps;
    }

    // ========== 헬퍼 함수 ==========

    // 현재 스텝 날짜: 이벤트가 있으면 이벤트 날짜, 다회 방문은 다음 방문일, 대기 단계는 없음
    private LocalDate currentDate(CaseStage stage, CaseEvent entryEvent, List<CaseEvent> events) {
        if (entryEvent != null) {
            return stepDate(entryEvent);
        }
        if (stage == CaseStage.REPAIR_IN_PROGRESS) {
            CaseEvent visited = findLatestEvent(events, CaseEventType.REPAIR_VISITED);
            return toDate(visited == null ? null : visited.getPayloadDateTime("nextVisitAt", storageZone));
        }
        return null;
    }

    // 점검·방문은 실제 일정 날짜, 나머지는 기록 시각
    private LocalDate stepDate(CaseEvent event) {
        LocalDateTime scheduledAt = switch (event.getType()) {
            case HPD_INSPECTION_SCHEDULED -> event.getPayloadDateTime("inspectionStart", storageZone);
            case REPAIR_VISITED -> event.getPayloadDateTime("visitedAt", storageZone);
            default -> null;
        };
        return toDate(scheduledAt != null ? scheduledAt : event.getOccurredAt());
    }

    private CaseEvent findLatestEvent(List<CaseEvent> events, CaseEventType type) {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).getType() == type) {
                return events.get(i);
            }
        }
        return null;
    }

    private String toOrdinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    private LocalDate toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(storageZone).withZoneSameInstant(CASE_ZONE).toLocalDate();
    }

    // DTO 변환 메소드
    private TimelineStepResponse toStep(String label, LocalDate date, TimelineStepState state) {
        return TimelineStepResponse.builder()
                .label(label)
                .date(date)
                .state(state)
                .build();
    }

    // stage별 현재/다음 스텝 정의
    private record StageStep(String currentLabel, TimelineStepState currentState, CaseEventType entryEvent, String nextLabel) {
    }
}
