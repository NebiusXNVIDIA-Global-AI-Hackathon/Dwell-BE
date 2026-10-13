package com.dwell.domain.cases.enums;

import com.dwell.global.common.enums.IssueCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// New Case Step 2 안전 경고 모달 (와프 문구 기준)
@Getter
@RequiredArgsConstructor
public enum SafetyAlert {

    ELECTRICAL(
            "Electrical Safety Alert",
            "This may be an electrical hazard",
            "Do not touch the affected area. Even if there is no immediate danger, contact your landlord right away. "
                    + "If you see smoke, fire, or another immediate danger, call 911."),

    GAS(
            "Gas Safety Alert",
            "A gas smell may indicate a gas leak.",
            "Leave the area immediately and move to a safe location. Once you're safe, call 911 or your gas utility's emergency line, "
                    + "and notify your landlord. Do not stay inside to take photos or collect evidence."),

    FLOODING(
            "Flooding Safety Alert",
            "Flooding may create an electrical hazard.",
            "If water is near outlets, wiring, or electrical equipment, do not enter or touch the affected area. "
                    + "Keep a safe distance and contact your landlord right away. If there is immediate danger, call 911.");

    private final String title;

    // 모달의 굵은 첫 줄
    private final String headline;

    private final String body;

    // 카테고리를 고르자마자 띄우는 경고 (Electricity, Gas). 세부 이슈 경고(Flooding)는 IssueDetail에 있음
    public static SafetyAlert ofCategory(IssueCategory category) {
        return switch (category) {
            case ELECTRICITY -> ELECTRICAL;
            case GAS -> GAS;
            default -> null;
        };
    }
}
