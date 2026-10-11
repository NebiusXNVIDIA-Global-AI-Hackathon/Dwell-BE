package com.dwell.domain.cases.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dwell.domain.cases.enums.CaseStage.*;

/*
클라이언트(POST /cases/{caseId}/events · Assistant 채팅 액션)가 기록할 수 있는 이벤트별 전이 규칙
- 도착 stage·payload는 API 명세, 허용 출발 stage는 와프 근거(플로우·배너 타임라인)가 있는 것만
- 와프 근거 없는 전이, RESOLVED·FOLLOW_UP_SENT는 구현 대기 (Dev/허용 전이 표)
- 여기 없는 이벤트(스케줄러·통지 발송·검증 API 전용)는 클라가 보낼 수 없음
 */
@Getter
public enum CaseEventRule {

    LANDLORD_REPLIED(CaseEventType.LANDLORD_REPLIED, SCHEDULING, false,
            EnumSet.of(AWAITING_RESPONSE, RESPONSE_OVERDUE, FOLLOW_UP_SENT, FOLLOW_UP_OVERDUE),
            List.of(), List.of("responseId")),

    REPAIR_SCHEDULED(CaseEventType.REPAIR_SCHEDULED, CaseStage.REPAIR_SCHEDULED, false,
            EnumSet.of(AWAITING_RESPONSE, RESPONSE_OVERDUE, FOLLOW_UP_SENT, FOLLOW_UP_OVERDUE, SCHEDULING),
            List.of("scheduledStart"), List.of("scheduledEnd", "repairedBy")),

    // nextVisitAt 없으면 stage 유지 (명세: 그대로 검증 대기)
    REPAIR_VISITED(CaseEventType.REPAIR_VISITED, REPAIR_IN_PROGRESS, false,
            EnumSet.of(CaseStage.REPAIR_SCHEDULED, REPAIR_DAY, REPAIR_IN_PROGRESS),
            List.of("visitedAt"), List.of("nextVisitAt")),

    LANDLORD_REFUSED(CaseEventType.LANDLORD_REFUSED, CaseStage.LANDLORD_REFUSED, true,
            EnumSet.of(AWAITING_RESPONSE, RESPONSE_OVERDUE, FOLLOW_UP_SENT, FOLLOW_UP_OVERDUE),
            List.of(), List.of()),

    LANDLORD_BLAME(CaseEventType.LANDLORD_BLAME, CaseStage.LANDLORD_BLAME, true,
            EnumSet.of(AWAITING_RESPONSE, RESPONSE_OVERDUE, FOLLOW_UP_SENT, FOLLOW_UP_OVERDUE),
            List.of(), List.of("claimReceivedAt")),

    COMPLAINT_311_FILED(CaseEventType.COMPLAINT_311_FILED, REPORTED_311, false,
            EnumSet.of(FOLLOW_UP_SENT, FOLLOW_UP_OVERDUE, CaseStage.LANDLORD_REFUSED, CaseStage.LANDLORD_BLAME, CaseStage.RECURRED),
            List.of("srNumber"), List.of()),

    HPD_INSPECTION_SCHEDULED(CaseEventType.HPD_INSPECTION_SCHEDULED, HPD_INSPECTION, false,
            EnumSet.of(REPORTED_311),
            List.of("inspectionStart"), List.of("inspectionEnd")),

    VIOLATION_ISSUED(CaseEventType.VIOLATION_ISSUED, VIOLATION_UNRESOLVED, true,
            EnumSet.of(HPD_INSPECTION),
            List.of("violationNumber"), List.of("violationClass")),

    SELF_REPAIR_STARTED(CaseEventType.SELF_REPAIR_STARTED, SELF_REPAIR, false,
            EnumSet.of(AWAITING_RESPONSE),
            List.of(), List.of("repairDate", "reason")),

    CLAIM_SENT(CaseEventType.CLAIM_SENT, CaseStage.CLAIM_SENT, false,
            EnumSet.of(SELF_REPAIR),
            List.of(), List.of("amount", "dueDate")),

    INSURANCE_CLAIM_FILED(CaseEventType.INSURANCE_CLAIM_FILED, INSURANCE_CLAIM, false,
            EnumSet.of(SELF_REPAIR, CaseStage.CLAIM_SENT),
            List.of(), List.of("claimNumber")),

    RECURRED(CaseEventType.RECURRED, CaseStage.RECURRED, false,
            EnumSet.of(RESOLVED),
            List.of(), List.of("recurredAt"));

    // 시각·날짜 값이라 형식을 검사하는 payload 키
    public static final Set<String> DATE_TIME_KEYS = Set.of(
            "scheduledStart", "scheduledEnd", "visitedAt", "nextVisitAt", "claimReceivedAt",
            "inspectionStart", "inspectionEnd", "repairDate", "dueDate", "recurredAt");

    private final CaseEventType type;
    private final CaseStage targetStage;
    private final boolean alert; // 진행 스텝 빨간 "!"
    private final Set<CaseStage> allowedFrom;
    private final List<String> requiredKeys;
    private final List<String> optionalKeys;

    CaseEventRule(CaseEventType type, CaseStage targetStage, boolean alert, Set<CaseStage> allowedFrom,
                  List<String> requiredKeys, List<String> optionalKeys) {
        this.type = type;
        this.targetStage = targetStage;
        this.alert = alert;
        this.allowedFrom = allowedFrom;
        this.requiredKeys = requiredKeys;
        this.optionalKeys = optionalKeys;
    }

    // 이벤트 type으로 규칙 찾기 (없으면 클라가 보낼 수 없는 이벤트)
    public static Optional<CaseEventRule> of(CaseEventType type) {
        return Arrays.stream(values()).filter(rule -> rule.type == type).findFirst();
    }

    public boolean canTransitFrom(CaseStage stage) {
        return allowedFrom.contains(stage);
    }

    // 도착 stage (REPAIR_VISITED는 다음 방문이 없으면 현재 stage 유지)
    public CaseStage nextStage(CaseStage current, Map<String, Object> payload) {
        if (this == REPAIR_VISITED && payload.get("nextVisitAt") == null) {
            return current;
        }
        return targetStage;
    }
}
