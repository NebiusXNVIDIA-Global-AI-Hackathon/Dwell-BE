package com.dwell.domain.cases.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 화면용 stage, PRD 상태머신용 status는 stage에서 파생
@Getter
@RequiredArgsConstructor
public enum CaseStage {
    LOGGED(CaseStatus.OPEN),
    AWAITING_RESPONSE(CaseStatus.WAITING),
    RESPONSE_OVERDUE(CaseStatus.ESCALATED),
    FOLLOW_UP_SENT(CaseStatus.WAITING),
    FOLLOW_UP_OVERDUE(CaseStatus.ESCALATED),
    SCHEDULING(CaseStatus.WAITING),
    REPAIR_SCHEDULED(CaseStatus.WAITING),
    REPAIR_DAY(CaseStatus.WAITING),
    REPAIR_IN_PROGRESS(CaseStatus.WAITING),
    LANDLORD_REFUSED(CaseStatus.DISPUTED),
    LANDLORD_BLAME(CaseStatus.DISPUTED),
    REPORTED_311(CaseStatus.DISPUTED),
    HPD_INSPECTION(CaseStatus.DISPUTED),
    VIOLATION_UNRESOLVED(CaseStatus.DISPUTED),
    SELF_REPAIR(CaseStatus.WAITING),
    CLAIM_SENT(CaseStatus.DISPUTED),
    INSURANCE_CLAIM(CaseStatus.WAITING),
    RESOLVED(CaseStatus.RESOLVED),
    RECURRED(CaseStatus.ESCALATED);

    private final CaseStatus status;

    // 상태 배지: LOGGED만 Logged, RESOLVED만 Resolved, 나머지는 In Progress
    public StatusBadge toStatusBadge() {
        if (this == LOGGED) {
            return StatusBadge.LOGGED;
        }
        if (this == RESOLVED) {
            return StatusBadge.RESOLVED;
        }
        return StatusBadge.IN_PROGRESS;
    }
}
