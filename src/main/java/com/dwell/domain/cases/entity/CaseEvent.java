package com.dwell.domain.cases.entity;

import com.dwell.domain.cases.enums.CaseEventRefType;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.global.common.LocalizedText;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// Case timeline entry
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseEvent {

    // payload에 오프셋 없이 들어온 시각·날짜는 뉴욕 현지 시각으로 해석
    private static final ZoneId CASE_ZONE = ZoneId.of("America/New_York");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseEventType type;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseEventRefType refType;

    // ID of the ref_type target (one row per evidence for EVIDENCE_ADDED)
    private Long refId;

    // Per type: {srNumber}, {violationNumber, violationClass}, {scheduledStart, scheduledEnd, repairedBy}, ...
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    // Highlighted as a warning on the timeline
    @Column(nullable = false)
    private boolean alert;

    @Builder
    public CaseEvent(Case caseEntity, CaseEventType type, LocalDateTime occurredAt, LocalizedText title,
                     CaseEventRefType refType, Long refId, Map<String, Object> payload, boolean alert) {
        this.caseEntity = caseEntity;
        this.type = type;
        this.occurredAt = occurredAt;
        this.title = title;
        this.refType = refType;
        this.refId = refId;
        this.payload = payload;
        this.alert = alert;
    }

    // payload 문자열 값 (srNumber, violationNumber 등)
    public String getPayloadString(String key) {
        if (payload == null || payload.get(key) == null) {
            return null;
        }
        return String.valueOf(payload.get(key));
    }

    // payload 시각 값을 저장 시간대(zone)의 LocalDateTime으로 (오프셋 포함 ISO, 오프셋 없는 ISO, 날짜만 모두 허용)
    // 오프셋이 없으면 뉴욕 현지 시각, 날짜만 있으면 뉴욕 자정으로 봄
    public LocalDateTime getPayloadDateTime(String key, ZoneId zone) {
        return parseDateTime(getPayloadString(key), zone);
    }

    // 시각 문자열 → 저장 시간대(zone)의 LocalDateTime (형식이 틀리면 null). 요청 검증에도 사용
    public static LocalDateTime parseDateTime(String value, ZoneId zone) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return fromCaseZone(LocalDateTime.parse(value), zone);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return fromCaseZone(LocalDate.parse(value).atStartOfDay(), zone);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    // 데모 시간 이동: 발생 시각과 payload 시각 값(keys)을 hours만큼 과거로 (형식은 원래대로 유지)
    public void shiftTimes(Collection<String> keys, long hours) {
        this.occurredAt = occurredAt.minusHours(hours);
        if (payload == null) {
            return;
        }
        Map<String, Object> shifted = new LinkedHashMap<>(payload);
        for (String key : keys) {
            if (shifted.get(key) != null) {
                shifted.put(key, shiftTime(String.valueOf(shifted.get(key)), hours));
            }
        }
        this.payload = shifted;
    }

    // 오프셋 포함 / 오프셋 없음 / 날짜만 (날짜만은 정오 기준으로 옮겨 24시간 단위로 날짜가 바뀜). 형식이 틀리면 그대로
    private static String shiftTime(String value, long hours) {
        try {
            return OffsetDateTime.parse(value).minusHours(hours).toString();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).minusHours(hours).toString();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value).atTime(12, 0).minusHours(hours).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    // 뉴욕 현지 시각 → 저장 시간대
    private static LocalDateTime fromCaseZone(LocalDateTime caseLocal, ZoneId zone) {
        return caseLocal.atZone(CASE_ZONE).withZoneSameInstant(zone).toLocalDateTime();
    }
}
