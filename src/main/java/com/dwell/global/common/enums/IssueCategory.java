package com.dwell.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueCategory {
    HEAT_HOT_WATER("Heat & Hot Water"),
    WATER_FLOODING("Water & Flooding"),
    PLUMBING("Plumbing"),
    MOLD_MOISTURE("Mold & Moisture"),
    PESTS("Pests"),
    ELECTRICITY("Electricity"),
    GAS("Gas"),
    APPLIANCES("Appliances"),
    VENTILATION("Ventilation"),
    SAFETY("Safety");

    // 화면 표시 문구 (New Case Step 1)
    private final String label;
}
