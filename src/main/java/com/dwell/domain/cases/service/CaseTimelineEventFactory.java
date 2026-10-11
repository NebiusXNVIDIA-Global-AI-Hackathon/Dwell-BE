package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.CaseTimelineEventResponse;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeChannel;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.MediaType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
Case Timeline 상세 카드 생성 (와프 찐최종 321:11015 기준)
설명 문장은 저장하지 않고 조회 시 템플릿으로 생성 (값은 이벤트 payload의 기록 시점 값 사용)
생성 때 올린 증거(INITIAL)는 Case Created 카드에 합침. 사진·영상 개수는 파일 단위 (증거 1건 = 파일 여러 개)
 */
@Component
public class CaseTimelineEventFactory {

    // 설명 문장의 날짜·시각은 뉴욕 현지 시각
    private static final ZoneId CASE_ZONE = ZoneId.of("America/New_York");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    // 카드 제목이 진행 스텝 라벨과 다른 것 (와프 타임라인 상세 문구)
    private static final Map<CaseEventType, String> TITLE_OVERRIDES = Map.of(
            CaseEventType.NOTICE_SENT, "Landlord Notified"
    );

    public List<CaseTimelineEventResponse> create(List<CaseEvent> events, List<Evidence> evidences, List<File> files, List<Notice> notices) {

        // 삭제되지 않은 증거, 통지
        Map<Long, Evidence> evidenceById = new HashMap<>();
        for (Evidence evidence : evidences) {
            evidenceById.put(evidence.getId(), evidence);
        }
        Map<Long, List<File>> filesByEvidenceId = new HashMap<>();
        for (File file : files) {
            filesByEvidenceId.computeIfAbsent(file.getEvidence().getId(), id -> new ArrayList<>()).add(file);
        }
        Map<Long, Notice> noticeById = new HashMap<>();
        for (Notice notice : notices) {
            noticeById.put(notice.getId(), notice);
        }

        // 1. 생성 때 올린 증거 → Case Created 카드로 (Case Created 이벤트가 없으면 따로 표시)
        CaseEvent createdEvent = events.stream()
                .filter(event -> event.getType() == CaseEventType.CASE_CREATED)
                .findFirst()
                .orElse(null);
        List<Evidence> initialEvidences = new ArrayList<>();
        if (createdEvent != null) {
            for (CaseEvent event : events) {
                Evidence evidence = evidenceOf(event, evidenceById);
                if (evidence != null && evidence.getPhase() == EvidencePhase.INITIAL) {
                    initialEvidences.add(evidence);
                }
            }
        }

        // 2. 카드 생성 (오래된 순, 방문은 1st Visit, 2nd Visit ...)
        List<CaseTimelineEventResponse> responses = new ArrayList<>();
        int visitCount = 0;
        for (CaseEvent event : events) {
            if (event.getType() == CaseEventType.EVIDENCE_ADDED) {
                Evidence evidence = evidenceOf(event, evidenceById);
                // 삭제된 증거, Case Created에 합친 증거는 카드 X
                if (evidence == null || initialEvidences.contains(evidence)) {
                    continue;
                }
                responses.add(toResponse(event, event.getType().getLabel(), toEvidenceAddedDescription(evidence, filesByEvidenceId), List.of(evidence.getId())));
                continue;
            }

            String title = TITLE_OVERRIDES.getOrDefault(event.getType(), event.getType().getLabel());
            if (event.getType() == CaseEventType.REPAIR_VISITED) {
                visitCount++;
                title = CaseTimelineStepFactory.toOrdinal(visitCount) + " Visit";
            }

            if (event == createdEvent) {
                responses.add(toResponse(event, title, toCreatedDescription(event, initialEvidences, filesByEvidenceId),
                        initialEvidences.stream().map(Evidence::getId).toList()));
                continue;
            }

            responses.add(toResponse(event, title, toDescription(event, noticeById), List.of()));
        }

        return responses;
    }

    // ========== 설명 문장 ==========

    // 타입별 설명 (값이 없으면 그 부분만 생략)
    private String toDescription(CaseEvent event, Map<Long, Notice> noticeById) {
        return switch (event.getType()) {
            case NOTICE_SENT -> "Sent" + toDelivery(event, noticeById) + ".";
            case FOLLOW_UP_SENT -> "Follow-up sent" + toDelivery(event, noticeById) + ".";
            case RESPONSE_OVERDUE -> "Your landlord didn't respond by the deadline.";
            case FOLLOW_UP_OVERDUE -> "Still no response after your follow-up.";
            case LANDLORD_REPLIED -> "Your landlord replied. A repair date isn't set yet.";
            case SCHEDULING -> "Working out a repair date with your landlord.";
            case REPAIR_SCHEDULED -> "Repair scheduled"
                    + prefixed(" for ", toRange(event, "scheduledStart", "scheduledEnd"))
                    + prefixed(" by ", event.getPayloadString("repairedBy")) + ".";
            case REPAIR_VISITED -> "Repair visit" + prefixed(" on ", toDate(event, "visitedAt")) + "."
                    + prefixed(" Next visit ", toDate(event, "nextVisitAt"), ".");
            case LANDLORD_REFUSED -> "Your landlord refused to make the repair.";
            case LANDLORD_BLAME -> "Your landlord said the problem is your fault.";
            case COMPLAINT_311_FILED -> "Filed a 311 complaint." + prefixed(" SR #", event.getPayloadString("srNumber"), ".");
            case HPD_INSPECTION_SCHEDULED -> "HPD inspection scheduled"
                    + prefixed(" for ", toRange(event, "inspectionStart", "inspectionEnd")) + ".";
            case VIOLATION_ISSUED -> "HPD issued a violation"
                    + prefixed(" #", event.getPayloadString("violationNumber"))
                    + prefixed(" (", toViolationClass(event.getPayloadString("violationClass")), ")") + ".";
            case SELF_REPAIR_STARTED -> "You started repairing it yourself" + prefixed(" on ", toDate(event, "repairDate")) + ".";
            case CLAIM_SENT -> "Sent a repair cost claim" + prefixed(" of ", toAmount(event.getPayload())) + " to your landlord."
                    + prefixed(" Due ", toDate(event, "dueDate"), ".");
            case INSURANCE_CLAIM_FILED -> "Filed an insurance claim" + prefixed(" #", event.getPayloadString("claimNumber")) + ".";
            case REPAIR_VERIFIED -> "The repair was verified.";
            case RESOLVED -> "The issue was resolved.";
            case RECURRED -> "The problem came back" + prefixed(" on ", toDate(event, "recurredAt")) + ".";
            case CASE_CREATED, EVIDENCE_ADDED -> null;
        };
    }

    // "You documented the issue with 2 photos and 1 video." (개수: payload 기록 값 → 없으면 합친 증거의 파일 수)
    private String toCreatedDescription(CaseEvent event, List<Evidence> initialEvidences, Map<Long, List<File>> filesByEvidenceId) {
        Integer photoCount = toInteger(event.getPayloadString("photoCount"));
        Integer videoCount = toInteger(event.getPayloadString("videoCount"));

        String counts;
        if (photoCount != null || videoCount != null) {
            List<String> parts = new ArrayList<>();
            addCount(parts, photoCount, "photo", "photos");
            addCount(parts, videoCount, "video", "videos");
            counts = joinWithAnd(parts);
        } else {
            counts = toFileCounts(initialEvidences, filesByEvidenceId);
        }

        return "You documented the issue" + prefixed(" with ", counts) + ".";
    }

    // "You added 2 photos after the repair."
    private String toEvidenceAddedDescription(Evidence evidence, Map<Long, List<File>> filesByEvidenceId) {
        String counts = toFileCounts(List.of(evidence), filesByEvidenceId);
        String phase = evidence.getPhase() == null ? null : switch (evidence.getPhase()) {
            case INITIAL -> null;
            case BEFORE_REPAIR -> "before the repair";
            case PROGRESS -> "during the repair";
            case AFTER_REPAIR -> "after the repair";
        };
        return "You added " + (counts == null ? "new evidence" : counts) + prefixed(" ", phase) + ".";
    }

    // 파일 종류별 개수 "2 photos and 1 video" (파일이 없는 증거는 증거의 미디어 타입으로 1개)
    private String toFileCounts(List<Evidence> evidences, Map<Long, List<File>> filesByEvidenceId) {
        Map<MediaType, Integer> counts = new EnumMap<>(MediaType.class);
        for (Evidence evidence : evidences) {
            List<File> files = filesByEvidenceId.getOrDefault(evidence.getId(), List.of());
            if (files.isEmpty()) {
                counts.merge(evidence.getMediaType() == null ? MediaType.MULTI : evidence.getMediaType(), 1, Integer::sum);
            }
            for (File file : files) {
                counts.merge(file.getMediaType() == null ? MediaType.MULTI : file.getMediaType(), 1, Integer::sum);
            }
        }

        List<String> parts = new ArrayList<>();
        addCount(parts, counts.get(MediaType.PHOTO), "photo", "photos");
        addCount(parts, counts.get(MediaType.VIDEO), "video", "videos");
        addCount(parts, counts.get(MediaType.AUDIO), "audio recording", "audio recordings");
        addCount(parts, counts.get(MediaType.DOCUMENT), "document", "documents");
        addCount(parts, counts.get(MediaType.MULTI), "file", "files");
        return joinWithAnd(parts);
    }

    // " by text to John Smith (Landlord)" (채널·받는 사람: payload 기록 값 → 없으면 통지의 채널)
    private String toDelivery(CaseEvent event, Map<Long, Notice> noticeById) {
        NoticeChannel channel = toChannel(event.getPayloadString("channel"));
        if (channel == null && event.getRefId() != null && noticeById.containsKey(event.getRefId())) {
            channel = noticeById.get(event.getRefId()).getChannel();
        }
        String by = channel == null ? null : switch (channel) {
            case SMS -> "by text";
            case EMAIL -> "by email";
            case MANUAL -> null;
        };

        String recipientName = event.getPayloadString("recipientName");
        String to = recipientName == null || recipientName.isBlank() ? "to your landlord" : "to " + recipientName + " (Landlord)";

        return prefixed(" ", by) + " " + to;
    }

    // ========== 값 포맷 ==========

    // "Oct 14, 10:00 AM – 12:00 PM" (끝이 다른 날이면 날짜 포함), 날짜만 있으면 "Oct 14"
    private String toRange(CaseEvent event, String startKey, String endKey) {
        String start = toDateTime(event, startKey);
        if (start == null) {
            return null;
        }
        LocalDateTime startAt = CaseEvent.parseDateTime(event.getPayloadString(startKey), CASE_ZONE);
        LocalDateTime endAt = CaseEvent.parseDateTime(event.getPayloadString(endKey), CASE_ZONE);
        if (endAt == null || isDateOnly(event.getPayloadString(startKey)) || isDateOnly(event.getPayloadString(endKey))) {
            return start;
        }
        return start + " – " + (endAt.toLocalDate().equals(startAt.toLocalDate()) ? TIME.format(endAt) : DATE_TIME.format(endAt));
    }

    private String toDateTime(CaseEvent event, String key) {
        String value = event.getPayloadString(key);
        LocalDateTime dateTime = CaseEvent.parseDateTime(value, CASE_ZONE);
        if (dateTime == null) {
            return null;
        }
        return isDateOnly(value) ? DATE.format(dateTime) : DATE_TIME.format(dateTime);
    }

    private String toDate(CaseEvent event, String key) {
        LocalDateTime dateTime = CaseEvent.parseDateTime(event.getPayloadString(key), CASE_ZONE);
        return dateTime == null ? null : DATE.format(dateTime);
    }

    private boolean isDateOnly(String value) {
        return value != null && !value.contains("T");
    }

    // 480 → "$480", 480.5 → "$480.50", 숫자가 아니면 그대로
    private String toAmount(Map<String, Object> payload) {
        Object value = payload == null ? null : payload.get("amount");
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value).replace("$", "").replace(",", "").trim());
            DecimalFormat format = new DecimalFormat(amount.stripTrailingZeros().scale() > 0 ? "$#,##0.00" : "$#,##0",
                    DecimalFormatSymbols.getInstance(Locale.US));
            return format.format(amount);
        } catch (NumberFormatException e) {
            return String.valueOf(value);
        }
    }

    // "C" → "Class C"
    private String toViolationClass(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT).startsWith("class") ? value : "Class " + value;
    }

    private NoticeChannel toChannel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return NoticeChannel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer toInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim()).intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addCount(List<String> parts, Integer count, String singular, String plural) {
        if (count != null && count > 0) {
            parts.add(count + " " + (count == 1 ? singular : plural));
        }
    }

    // ["2 photos", "1 video"] → "2 photos and 1 video"
    private String joinWithAnd(List<String> parts) {
        if (parts.isEmpty()) {
            return null;
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    private String prefixed(String prefix, String value) {
        return prefixed(prefix, value, "");
    }

    // 값이 있을 때만 앞뒤 문구를 붙임
    private String prefixed(String prefix, String value, String suffix) {
        return value == null || value.isBlank() ? "" : prefix + value + suffix;
    }

    // ========== 헬퍼 함수 ==========

    private Evidence evidenceOf(CaseEvent event, Map<Long, Evidence> evidenceById) {
        if (event.getType() != CaseEventType.EVIDENCE_ADDED || event.getRefId() == null) {
            return null;
        }
        return evidenceById.get(event.getRefId());
    }

    // 메모(note)는 사용자 원문이라 meta에서 제외
    private Map<String, Object> toMeta(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Map<String, Object> meta = new LinkedHashMap<>(payload);
        meta.remove("note");
        return meta.isEmpty() ? null : meta;
    }

    // DTO 변환 메소드
    private CaseTimelineEventResponse toResponse(CaseEvent event, String title, String description, List<Long> evidenceIds) {
        return CaseTimelineEventResponse.builder()
                .eventId(event.getId())
                .type(event.getType())
                .occurredAt(event.getOccurredAt())
                .title(new LocalizedText(title, null))
                .description(description == null ? null : new LocalizedText(description, null))
                .refId(event.getRefId())
                .evidenceIds(evidenceIds)
                .meta(toMeta(event.getPayload()))
                .alert(event.isAlert())
                .build();
    }
}
