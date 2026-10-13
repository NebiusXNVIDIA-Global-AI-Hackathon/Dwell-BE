package com.dwell.domain.cases.enums;

import com.dwell.global.common.enums.Location;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum AffectedArea {

    // 기존 값 (이름 변경 금지)
    CEILING("Ceiling"),
    WALL("Wall"),
    FLOOR("Floor"),
    SHOWER("Shower / Tub"),
    SINK("Sink"),
    TOILET("Toilet"),
    WINDOW_AREA("Window Area"),
    DOOR_AREA("Door Area"),
    UNDER_SINK("Under Sink"),
    VENTILATION("Ventilation"),
    OTHER("Other"),

    // Kitchen
    COUNTERTOP("Countertop"),
    CABINET("Cabinet"),
    APPLIANCE_AREA("Appliance Area"),

    // Living Room, Bedroom
    SOFA_SEATING("Sofa / Seating"),
    ELECTRICAL_OUTLET_FIXTURE("Electrical Outlet / Fixture"),
    LIGHT_FIXTURE("Light Fixture"),
    HVAC_VENT("HVAC / Vent"),
    FIREPLACE("Fireplace"),
    BUILT_IN_SHELF("Built-in / Shelf"),
    CLOSET("Closet"),
    BASEBOARD("Baseboard"),

    // Laundry Area
    WASHER_AREA("Washer Area"),
    DRYER_AREA("Dryer Area"),
    LAUNDRY_SINK("Laundry Sink"),
    DRAIN("Drain"),
    PLUMBING_PIPE("Plumbing / Pipe"),
    STORAGE_SHELF("Storage / Shelf"),

    // Balcony
    RAILING("Railing"),
    EXTERIOR_FIXTURE("Exterior Fixture"),
    OUTDOOR_OUTLET("Outdoor Outlet"),
    PLANTER_AREA("Planter Area"),
    STAIRS_ENTRYWAY("Stairs / Entryway"),

    // Exterior
    ROOF_OVERHANG("Roof / Overhang"),
    BALCONY_TERRACE("Balcony / Terrace"),
    DRAIN_GUTTER("Drain / Gutter"),
    EXTERIOR_PIPE("Exterior Pipe"),
    FOUNDATION("Foundation"),
    WALKWAY_GROUND("Walkway / Ground");

    // 화면 표시 문구 (New Case Step 4)
    private final String label;

    // 공간에서 고를 수 있는 부위 (와프 Step 4 화면 순서)
    public static List<AffectedArea> allowedIn(Location location) {
        return switch (location) {
            case BATHROOM -> List.of(CEILING, WALL, FLOOR, SHOWER, SINK, TOILET, WINDOW_AREA, DOOR_AREA,
                    UNDER_SINK, VENTILATION, OTHER);

            case KITCHEN -> List.of(CEILING, WALL, FLOOR, SINK, UNDER_SINK, COUNTERTOP, CABINET, APPLIANCE_AREA,
                    VENTILATION, WINDOW_AREA, DOOR_AREA, OTHER);

            case LIVING_ROOM -> List.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, SOFA_SEATING,
                    ELECTRICAL_OUTLET_FIXTURE, LIGHT_FIXTURE, HVAC_VENT, FIREPLACE, BUILT_IN_SHELF, OTHER);

            case BEDROOM -> List.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, CLOSET,
                    ELECTRICAL_OUTLET_FIXTURE, LIGHT_FIXTURE, HVAC_VENT, BASEBOARD, BUILT_IN_SHELF, OTHER);

            case LAUNDRY_AREA -> List.of(CEILING, WALL, FLOOR, WASHER_AREA, DRYER_AREA, LAUNDRY_SINK, DRAIN,
                    PLUMBING_PIPE, VENTILATION, DOOR_AREA, STORAGE_SHELF, OTHER);

            case BALCONY -> List.of(CEILING, WALL, FLOOR, RAILING, DOOR_AREA, WINDOW_AREA, DRAIN,
                    EXTERIOR_FIXTURE, OUTDOOR_OUTLET, PLANTER_AREA, STAIRS_ENTRYWAY, OTHER);

            case EXTERIOR -> List.of(WALL, ROOF_OVERHANG, WINDOW_AREA, DOOR_AREA, BALCONY_TERRACE, DRAIN_GUTTER,
                    EXTERIOR_PIPE, STAIRS_ENTRYWAY, FOUNDATION, EXTERIOR_FIXTURE, WALKWAY_GROUND, OTHER);

            // 와프에 없는 공간(Hallway)과 직접 입력한 공간(Other)은 공통 부위만
            case HALLWAY, OTHER -> List.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER);

            // Entire Unit은 부위를 고르지 않음 (Step 4 건너뜀)
            case ENTIRE_UNIT -> List.of();
        };
    }

    // 해당 공간에서 고를 수 있는 부위인지
    public boolean isAllowedIn(Location location) {
        return allowedIn(location).contains(this);
    }
}
