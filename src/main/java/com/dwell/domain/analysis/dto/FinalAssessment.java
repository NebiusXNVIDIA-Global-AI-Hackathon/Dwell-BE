package com.dwell.domain.analysis.dto;

import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.dwell.global.common.LocalizedText;

import java.util.List;

/*
Nemotron(Case Agent)의 최종 판정 — 서버 규칙(SeverityPolicy)을 거치기 전 값
- progression: SPREADING | STABLE | IMPROVING | NOT_APPLICABLE
- severityReasons: "Why it's high severity" 화면용 근거 2~3개 (en + 사용자 언어)
 */
public record FinalAssessment(AssessedSeverity finalSeverity, String adjustmentReason, String adjustmentSource,
                              String progression, String progressionReason, List<LocalizedText> severityReasons) {
}
