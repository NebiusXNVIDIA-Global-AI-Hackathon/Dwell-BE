package com.dwell.domain.analysis.event;

// 케이스 생성·증거 추가 후 AI 심각도 분석 요청 (커밋 후 비동기 실행)
public record CaseAnalysisRequestedEvent(Long caseId) {
}
