package com.dwell.domain.analysis.dto;

import com.dwell.domain.analysis.enums.AssessedSeverity;

import java.util.List;

// 서버 규칙을 적용한 최종 결과 (corrections: 모델 값을 규칙 범위로 고친 내역)
public record PolicyResult(AssessedSeverity finalSeverity, AssessedSeverity visionSeverity, boolean adjusted,
                           boolean needsRetake, Integer proposedSlaHours, List<String> corrections) {
}
