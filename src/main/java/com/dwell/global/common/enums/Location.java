package com.dwell.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Location {
    BATHROOM("Bathroom"),
    KITCHEN("Kitchen"),
    LIVING_ROOM("Living Room"),
    HALLWAY("Hallway"),
    BEDROOM("Bedroom"),
    BALCONY("Balcony"),
    EXTERIOR("Exterior"),
    ENTIRE_UNIT("Entire Unit"),
    OTHER("Other"),
    LAUNDRY_AREA("Laundry Area");

    // 화면 표시 문구 (New Case Step 3)
    private final String label;
}
