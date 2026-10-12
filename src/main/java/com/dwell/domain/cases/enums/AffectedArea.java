package com.dwell.domain.cases.enums;

import com.dwell.global.common.enums.Location;

import java.util.Set;

public enum AffectedArea {

    // 기존 값 (이름 변경 금지)
    CEILING,
    WALL,
    FLOOR,
    SHOWER,
    SINK,
    TOILET,
    WINDOW_AREA,
    DOOR_AREA,
    UNDER_SINK,
    VENTILATION,
    OTHER,

    // Kitchen
    COUNTERTOP,
    CABINET,
    APPLIANCE_AREA,

    // Living Room, Bedroom
    SOFA_SEATING,
    ELECTRICAL_OUTLET_FIXTURE,
    LIGHT_FIXTURE,
    HVAC_VENT,
    FIREPLACE,
    BUILT_IN_SHELF,
    CLOSET,
    BASEBOARD,

    // Laundry Area
    WASHER_AREA,
    DRYER_AREA,
    LAUNDRY_SINK,
    DRAIN,
    PLUMBING_PIPE,
    STORAGE_SHELF,

    // Balcony
    RAILING,
    EXTERIOR_FIXTURE,
    OUTDOOR_OUTLET,
    PLANTER_AREA,
    STAIRS_ENTRYWAY,

    // Exterior
    ROOF_OVERHANG,
    BALCONY_TERRACE,
    DRAIN_GUTTER,
    EXTERIOR_PIPE,
    FOUNDATION,
    WALKWAY_GROUND;

    // 해당 공간에서 고를 수 있는 부위인지
    public boolean isAllowedIn(Location location) {
        Set<AffectedArea> allowedAreas = switch (location) {
            case BATHROOM -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER,
                    SHOWER, SINK, TOILET, UNDER_SINK, VENTILATION);

            case KITCHEN -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER,
                    SINK, UNDER_SINK, COUNTERTOP, CABINET, APPLIANCE_AREA, VENTILATION);

            case LIVING_ROOM -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER,
                    SOFA_SEATING, ELECTRICAL_OUTLET_FIXTURE, LIGHT_FIXTURE, HVAC_VENT, FIREPLACE, BUILT_IN_SHELF);

            case BEDROOM -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER,
                    CLOSET, ELECTRICAL_OUTLET_FIXTURE, LIGHT_FIXTURE, HVAC_VENT, BASEBOARD, BUILT_IN_SHELF);

            case LAUNDRY_AREA -> Set.of(CEILING, WALL, FLOOR, DOOR_AREA, OTHER,
                    WASHER_AREA, DRYER_AREA, LAUNDRY_SINK, DRAIN, PLUMBING_PIPE, VENTILATION, STORAGE_SHELF);

            case BALCONY -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER,
                    RAILING, DRAIN, EXTERIOR_FIXTURE, OUTDOOR_OUTLET, PLANTER_AREA, STAIRS_ENTRYWAY);

            case EXTERIOR -> Set.of(WALL, WINDOW_AREA, DOOR_AREA, OTHER,
                    ROOF_OVERHANG, BALCONY_TERRACE, DRAIN_GUTTER, EXTERIOR_PIPE, STAIRS_ENTRYWAY,
                    FOUNDATION, EXTERIOR_FIXTURE, WALKWAY_GROUND);

            // 와프에 없는 공간(Hallway)과 직접 입력한 공간(Other)은 공통 부위만
            case HALLWAY, OTHER -> Set.of(CEILING, WALL, FLOOR, WINDOW_AREA, DOOR_AREA, OTHER);

            // Entire Unit은 부위를 고르지 않음
            case ENTIRE_UNIT -> Set.of();
        };

        return allowedAreas.contains(this);
    }
}