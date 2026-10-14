package com.dwell.domain.analysis.enums;

import com.dwell.global.common.enums.Severity;

// 분석 중 쓰는 심각도 (Inconclusive 포함). 저장할 때는 Severity로 바꾸고 Inconclusive는 null
public enum AssessedSeverity {
    INCONCLUSIVE,
    LOW,
    MEDIUM,
    HIGH;

    // 모델 출력("High", "medium" 등) -> enum, 모르는 값이면 null
    public static AssessedSeverity parse(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        try {
            return valueOf(text.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // LOW < MEDIUM < HIGH 순서 (INCONCLUSIVE는 비교 대상 아님)
    public int rank() {
        return ordinal();
    }

    public AssessedSeverity oneLower() {
        return this == HIGH ? MEDIUM : LOW;
    }

    public Severity toSeverity() {
        return this == INCONCLUSIVE ? null : Severity.valueOf(name());
    }

    public static AssessedSeverity of(Severity severity) {
        return severity == null ? INCONCLUSIVE : valueOf(severity.name());
    }
}
