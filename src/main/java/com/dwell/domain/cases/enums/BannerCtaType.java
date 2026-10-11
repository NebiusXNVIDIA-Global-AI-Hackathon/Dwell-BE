package com.dwell.domain.cases.enums;

// Current Status 배너 버튼 동작
public enum BannerCtaType {
    SEND_NOTICE,      // 집주인 메시지 작성
    UPDATE_STATUS,    // 케이스 채팅으로 이동
    TALK_TO_DEAVER,   // 케이스 채팅으로 이동
    ADD_TO_CALENDAR,  // 클라 네이티브 캘린더
    ADD_EVIDENCE,     // 증거 추가
    ESCALATE,         // 케이스 채팅 - 에스컬레이션
    SEND_CLAIM,       // 케이스 채팅 - 수리비 청구
    EXPORT_REPORT     // 케이스 리포트 생성
}
