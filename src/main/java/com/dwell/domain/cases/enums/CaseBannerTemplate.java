package com.dwell.domain.cases.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// stage별 Current Status 배너 고정 문구 (와프 찐최종 기준)
@Getter
@RequiredArgsConstructor
public enum CaseBannerTemplate {
    LOGGED(CaseStage.LOGGED, "Ready to notify your landlord", BannerTone.DEFAULT, BannerCtaType.SEND_NOTICE, "Send a notice"),
    AWAITING_RESPONSE(CaseStage.AWAITING_RESPONSE, "Waiting for your landlord's response", BannerTone.DEFAULT, BannerCtaType.UPDATE_STATUS, "Got a Reply? Update Status"),
    RESPONSE_OVERDUE(CaseStage.RESPONSE_OVERDUE, "Did you get a reply?", BannerTone.DEFAULT, BannerCtaType.UPDATE_STATUS, "Update status"),
    FOLLOW_UP_SENT(CaseStage.FOLLOW_UP_SENT, "Waiting for your landlord's response", BannerTone.DEFAULT, BannerCtaType.UPDATE_STATUS, "Got a Reply? Update Status"),
    FOLLOW_UP_OVERDUE(CaseStage.FOLLOW_UP_OVERDUE, "Still no response", BannerTone.DEFAULT, BannerCtaType.TALK_TO_DEAVER, "Talk to Deaver"),
    SCHEDULING(CaseStage.SCHEDULING, "Setting up a repair date", BannerTone.DEFAULT, BannerCtaType.UPDATE_STATUS, "Update status"),
    REPAIR_SCHEDULED(CaseStage.REPAIR_SCHEDULED, "Waiting For Repair", BannerTone.DEFAULT, BannerCtaType.ADD_TO_CALENDAR, "Add to calendar"),
    REPAIR_DAY(CaseStage.REPAIR_DAY, "Repair D-Day", BannerTone.DEFAULT, BannerCtaType.ADD_EVIDENCE, "Add before & after photos"),
    REPAIR_IN_PROGRESS(CaseStage.REPAIR_IN_PROGRESS, "Repair in Progress", BannerTone.DEFAULT, BannerCtaType.ADD_EVIDENCE, "Add Progress Photos"),
    LANDLORD_REFUSED(CaseStage.LANDLORD_REFUSED, "Landlord isn't fixing it", BannerTone.DANGER, BannerCtaType.ESCALATE, "Time to Escalate"),
    LANDLORD_BLAME(CaseStage.LANDLORD_BLAME, "Landlord says it's your fault", BannerTone.DANGER, BannerCtaType.ESCALATE, "Time to Escalate"),
    REPORTED_311(CaseStage.REPORTED_311, "Reported to 311", BannerTone.DANGER, BannerCtaType.TALK_TO_DEAVER, "Talk to Deaver"),
    HPD_INSPECTION(CaseStage.HPD_INSPECTION, "HPD inspection scheduled", BannerTone.DANGER, BannerCtaType.TALK_TO_DEAVER, "Talk to Deaver"),
    VIOLATION_UNRESOLVED(CaseStage.VIOLATION_UNRESOLVED, "Violation issued, still not fixed", BannerTone.DANGER, BannerCtaType.TALK_TO_DEAVER, "Talk to Deaver"),
    SELF_REPAIR(CaseStage.SELF_REPAIR, "Repairing it yourself", BannerTone.DEFAULT, BannerCtaType.ADD_EVIDENCE, "Add Progress Photos"),
    CLAIM_SENT(CaseStage.CLAIM_SENT, "Filing a claim", BannerTone.DEFAULT, BannerCtaType.SEND_CLAIM, "Send a Claim"),
    INSURANCE_CLAIM(CaseStage.INSURANCE_CLAIM, "Filing an insurance claim", BannerTone.DEFAULT, BannerCtaType.UPDATE_STATUS, "Update status"),
    RESOLVED(CaseStage.RESOLVED, "Resolved", BannerTone.SUCCESS, BannerCtaType.EXPORT_REPORT, "Save case report"),
    RECURRED(CaseStage.RECURRED, "The problem recurred", BannerTone.RECURRED, BannerCtaType.TALK_TO_DEAVER, "Trace with Deaver");

    private final CaseStage stage;
    private final String title;
    private final BannerTone tone;
    private final BannerCtaType ctaType;
    private final String ctaLabel;

    // stage로 템플릿 찾기 (enum 이름이 stage와 1:1)
    public static CaseBannerTemplate of(CaseStage stage) {
        return valueOf(stage.name());
    }
}
