package com.dwell.domain.cases.enums;

// Case Detail 진행 스텝 상태
public enum TimelineStepState {
    DONE,
    CURRENT,
    TODO,
    ALERT  // 빨간 ! (Response Overdue, Landlord Refused 등)
}
