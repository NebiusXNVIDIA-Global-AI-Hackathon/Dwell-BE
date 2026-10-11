package com.dwell.domain.cases.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// label = 진행 스텝·이벤트 제목 문구 (와프 찐최종 기준)
@Getter
@RequiredArgsConstructor
public enum CaseEventType {
    CASE_CREATED("Case Created"),
    EVIDENCE_ADDED("Evidence Added"),
    NOTICE_SENT("Notified Landlord"),
    FOLLOW_UP_SENT("Follow-up Sent"),
    RESPONSE_OVERDUE("No Response"),
    FOLLOW_UP_OVERDUE("Still no Response"), // 재통지 기한 경과 (스케줄러 기록)
    LANDLORD_REPLIED("Landlord Replied"),
    SCHEDULING("Scheduling"),
    REPAIR_SCHEDULED("Repair Scheduled"),
    REPAIR_VISITED("Visit"), // 진행 스텝에서는 1st Visit, 2nd Visit ...
    LANDLORD_REFUSED("Landlord Refused"),
    LANDLORD_BLAME("Landlord Blame Shifting"),
    COMPLAINT_311_FILED("311 Filed"),
    HPD_INSPECTION_SCHEDULED("HPD Inspection"),
    VIOLATION_ISSUED("Violation Issued"),
    SELF_REPAIR_STARTED("Repairing Myself"),
    CLAIM_SENT("Claim Sent"),
    INSURANCE_CLAIM_FILED("Claim Filed"),
    REPAIR_VERIFIED("Repaired"),
    RESOLVED("Resolved"),
    RECURRED("Recurred");

    private final String label;
}
