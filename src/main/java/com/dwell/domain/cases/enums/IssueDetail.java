package com.dwell.domain.cases.enums;

import com.dwell.global.common.enums.IssueCategory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// New Case Step 2 세부 이슈 (와프 "이슈별 세부이슈 값" 표 기준, Case.issueDetail에 코드로 저장)
@Getter
public enum IssueDetail {

    // Heat & Hot Water
    NO_HEAT(IssueCategory.HEAT_HOT_WATER, "No Heat"),
    NOT_ENOUGH_HEAT(IssueCategory.HEAT_HOT_WATER, "Not Enough Heat"),
    NO_HOT_WATER(IssueCategory.HEAT_HOT_WATER, "No Hot Water"),
    WATER_NOT_HOT_ENOUGH(IssueCategory.HEAT_HOT_WATER, "Water Not Hot Enough"),
    HEAT_WONT_TURN_OFF(IssueCategory.HEAT_HOT_WATER, "Heat Won't Turn Off"),
    RADIATOR_ISSUE(IssueCategory.HEAT_HOT_WATER, "Radiator Issue"),

    // Water & Flooding
    WATER_LEAK(IssueCategory.WATER_FLOODING, "Water Leak"),
    WATER_STAIN(IssueCategory.WATER_FLOODING, "Water Stain or Damage"),
    ACTIVE_DRIPPING(IssueCategory.WATER_FLOODING, "Active Dripping"),
    FLOODING(IssueCategory.WATER_FLOODING, "Flooding", SafetyAlert.FLOODING),
    WATER_ON_FLOOR(IssueCategory.WATER_FLOODING, "Water on Floor"),

    // Plumbing
    CLOGGED_DRAIN(IssueCategory.PLUMBING, "Clogged Drain"),
    TOILET_ISSUE(IssueCategory.PLUMBING, "Toilet Issue"),
    SINK_ISSUE(IssueCategory.PLUMBING, "Sink Issue"),
    LOW_WATER_PRESSURE(IssueCategory.PLUMBING, "Low Water Pressure"),
    PIPE_ISSUE(IssueCategory.PLUMBING, "Pipe Issue"),
    FAUCET_ISSUE(IssueCategory.PLUMBING, "Faucet Issue"),
    SEWAGE_BACKUP(IssueCategory.PLUMBING, "Sewage Backup"),

    // Mold & Moisture
    VISIBLE_MOLD(IssueCategory.MOLD_MOISTURE, "Visible Mold"),
    DAMP_WALL_OR_CEILING(IssueCategory.MOLD_MOISTURE, "Damp Wall or Ceiling"),
    MUSTY_SMELL(IssueCategory.MOLD_MOISTURE, "Musty Smell"),
    CONDENSATION(IssueCategory.MOLD_MOISTURE, "Condensation"),
    RECURRING_MOISTURE(IssueCategory.MOLD_MOISTURE, "Recurring Moisture"),
    PEELING_PAINT(IssueCategory.MOLD_MOISTURE, "Peeling Paint"),

    // Pests
    ROACHES(IssueCategory.PESTS, "Roaches"),
    MICE_RATS(IssueCategory.PESTS, "Mice / Rats"),
    BED_BUGS(IssueCategory.PESTS, "Bed Bugs"),
    ANTS(IssueCategory.PESTS, "Ants"),
    FLIES_GNATS(IssueCategory.PESTS, "Flies / Gnats"),
    OTHER_INSECTS(IssueCategory.PESTS, "Other Insects"),

    // Electricity
    NO_POWER(IssueCategory.ELECTRICITY, "No Power"),
    OUTLET_NOT_WORKING(IssueCategory.ELECTRICITY, "Outlet Not Working"),
    LIGHT_FIXTURE_ISSUE(IssueCategory.ELECTRICITY, "Light / Fixture Issue"),
    FLICKERING_LIGHTS(IssueCategory.ELECTRICITY, "Flickering Lights"),
    BREAKER_KEEPS_TRIPPING(IssueCategory.ELECTRICITY, "Breaker Keeps Tripping"),
    EXPOSED_WIRING(IssueCategory.ELECTRICITY, "Exposed Wiring"),
    SPARKS_BURNING_SMELL(IssueCategory.ELECTRICITY, "Sparks / Burning Smell"),

    // Gas
    GAS_SMELL(IssueCategory.GAS, "Gas Smell"),
    SUSPECTED_GAS_LEAK(IssueCategory.GAS, "Suspected Gas Leak"),
    GAS_SHUT_OFF(IssueCategory.GAS, "Gas Shut Off"),
    GAS_APPLIANCE_ISSUE(IssueCategory.GAS, "Gas Appliance Issue"),
    GAS_LINE_PIPE_ISSUE(IssueCategory.GAS, "Gas Line / Pipe Issue"),

    // Appliances
    REFRIGERATOR(IssueCategory.APPLIANCES, "Refrigerator"),
    STOVE_OVEN(IssueCategory.APPLIANCES, "Stove / Oven"),
    DISHWASHER(IssueCategory.APPLIANCES, "Dishwasher"),
    WASHER(IssueCategory.APPLIANCES, "Washer"),
    DRYER(IssueCategory.APPLIANCES, "Dryer"),
    MICROWAVE(IssueCategory.APPLIANCES, "Microwave"),

    // Ventilation
    VENT_NOT_WORKING(IssueCategory.VENTILATION, "Vent Not Working"),
    WEAK_AIRFLOW(IssueCategory.VENTILATION, "Weak Airflow"),
    EXHAUST_FAN_ISSUE(IssueCategory.VENTILATION, "Exhaust Fan Issue"),
    BLOCKED_VENT(IssueCategory.VENTILATION, "Blocked Vent"),
    EXCESSIVE_HUMIDITY(IssueCategory.VENTILATION, "Excessive Humidity"),
    BAD_ODOR_POOR_AIRFLOW(IssueCategory.VENTILATION, "Bad Odor / Poor Airflow"),

    // Safety
    BROKEN_LOCK(IssueCategory.SAFETY, "Broken Lock"),
    DOOR_WONT_SECURE(IssueCategory.SAFETY, "Door Won't Secure"),
    WINDOW_WONT_LOCK(IssueCategory.SAFETY, "Window Won't Lock"),
    SMOKE_DETECTOR_ISSUE(IssueCategory.SAFETY, "Smoke Detector Issue"),
    CO_DETECTOR_ISSUE(IssueCategory.SAFETY, "Carbon Monoxide Detector"),
    BROKEN_UNSAFE_STAIRS(IssueCategory.SAFETY, "Broken / Unsafe Stairs"),
    LOOSE_RAILING(IssueCategory.SAFETY, "Loose Railing");

    // 직접 입력 선택지 코드 (모든 카테고리 공통이라 enum 값으로 두지 않음)
    public static final String OTHER = "OTHER";

    private final IssueCategory category;
    private final String label;

    // 고르면 띄우는 안전 경고 (없으면 null)
    private final SafetyAlert safetyAlert;

    IssueDetail(IssueCategory category, String label) {
        this(category, label, null);
    }

    IssueDetail(IssueCategory category, String label, SafetyAlert safetyAlert) {
        this.category = category;
        this.label = label;
        this.safetyAlert = safetyAlert;
    }

    // 카테고리의 세부 이슈 (와프 순서, OTHER 제외)
    public static List<IssueDetail> detailsOf(IssueCategory category) {
        List<IssueDetail> details = new ArrayList<>();
        for (IssueDetail detail : values()) {
            if (detail.category == category) {
                details.add(detail);
            }
        }
        return details;
    }

    // 카테고리별 직접 입력 선택지 문구 (Appliances만 "Other Appliance")
    public static String otherLabel(IssueCategory category) {
        if (category == IssueCategory.APPLIANCES) {
            return "Other Appliance";
        }
        return "Other";
    }

    // 카테고리에서 고를 수 있는 세부 이슈 코드인지 (OTHER는 모든 카테고리에서 허용)
    public static boolean isAllowedIn(IssueCategory category, String code) {
        if (OTHER.equals(code)) {
            return true;
        }
        for (IssueDetail detail : values()) {
            if (detail.name().equals(code)) {
                return detail.category == category;
            }
        }
        return false;
    }
}
