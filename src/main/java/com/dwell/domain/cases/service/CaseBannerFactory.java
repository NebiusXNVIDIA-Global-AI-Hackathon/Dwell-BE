package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.CurrentStatusResponse;
import com.dwell.domain.cases.dto.response.StatusCtaResponse;
import com.dwell.domain.cases.dto.response.StatusLineResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseBannerTemplate;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeType;
import com.dwell.global.common.LocalizedText;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/*
Current Status 배너 생성
고정 문구(제목·색·버튼)는 CaseBannerTemplate, 날짜·번호 같은 값은 타임라인 이벤트와 발송된 Notice에서 채움
조회 시점에 계산하므로 LLM 호출이나 DB 저장 없음
 */
@Component
public class CaseBannerFactory {

    // D-day는 뉴욕 날짜 기준
    private static final ZoneId CASE_ZONE = ZoneId.of("America/New_York");

    // DB의 LocalDateTime은 JVM 기본 시간대로 저장됨 (DwellApplication에서 America/New_York으로 고정)
    private final ZoneId storageZone;

    public CaseBannerFactory() {
        this(ZoneId.systemDefault());
    }

    // 테스트에서 시간대 고정용
    CaseBannerFactory(ZoneId storageZone) {
        this.storageZone = storageZone;
    }

    public CurrentStatusResponse create(Case caseEntity, List<CaseEvent> events, List<Notice> sentNotices, LocalDateTime now) {

        CaseBannerTemplate template = CaseBannerTemplate.of(caseEntity.getStage());

        // 가장 최근 발송된 1차 통지 / 재통지
        Notice firstNotice = findLatestNotice(sentNotices, NoticeType.FIRST);
        Notice followUpNotice = findLatestNotice(sentNotices, NoticeType.FOLLOW_UP);

        List<StatusLineResponse> lines = new ArrayList<>();
        Integer dDay = null;
        LocalDateTime startAt = null;
        LocalDateTime dueAt = null;

        switch (caseEntity.getStage()) {
            case LOGGED -> addLine(lines, "Case created", null, caseEntity.getCreatedAt(), null);

            // 진행바: 통지 발송 ~ 응답 기한
            case AWAITING_RESPONSE -> {
                addLine(lines, "Notice sent", null, sentAt(firstNotice), null);
                addLine(lines, "Response due", null, slaDueAt(firstNotice), null);
                startAt = sentAt(firstNotice);
                dueAt = slaDueAt(firstNotice);
            }

            // D+1 Did you get a reply?
            case RESPONSE_OVERDUE -> {
                addLine(lines, "Notice sent", null, sentAt(firstNotice), null);
                dueAt = slaDueAt(firstNotice);
                if (dueAt != null) {
                    dDay = (int) Math.max(1, daysBetween(dueAt, now));
                }
            }

            case FOLLOW_UP_SENT -> {
                addLine(lines, "Follow-up sent", null, sentAt(followUpNotice), null);
                addLine(lines, "Response due", null, slaDueAt(followUpNotice), null);
                startAt = sentAt(followUpNotice);
                dueAt = slaDueAt(followUpNotice);
            }

            // "8 hours past due"는 클라가 dueAt으로 계산
            case FOLLOW_UP_OVERDUE -> {
                addLine(lines, "Follow-up sent", null, sentAt(followUpNotice), null);
                dueAt = slaDueAt(followUpNotice);
            }

            case SCHEDULING -> {
                CaseEvent replied = findLatestEvent(events, CaseEventType.LANDLORD_REPLIED);
                addLine(lines, "Talking with landlord since", null, occurredAt(replied), null);
            }

            // D-3 Waiting For Repair
            case REPAIR_SCHEDULED -> {
                CaseEvent scheduled = findLatestEvent(events, CaseEventType.REPAIR_SCHEDULED);
                LocalDateTime scheduledStart = payloadDateTime(scheduled, "scheduledStart");
                addLine(lines, "Scheduled", null, scheduledStart, payloadDateTime(scheduled, "scheduledEnd"));
                if (scheduledStart != null) {
                    dDay = (int) -Math.max(0, daysBetween(now, scheduledStart));
                }
            }

            case REPAIR_DAY -> {
                CaseEvent scheduled = findLatestEvent(events, CaseEventType.REPAIR_SCHEDULED);
                addLine(lines, "Scheduled", null, payloadDateTime(scheduled, "scheduledStart"), payloadDateTime(scheduled, "scheduledEnd"));
                dDay = 0;
            }

            case REPAIR_IN_PROGRESS -> {
                CaseEvent visited = findLatestEvent(events, CaseEventType.REPAIR_VISITED);
                LocalDateTime visitedAt = payloadDateTime(visited, "visitedAt");
                addLine(lines, "Visited", null, visitedAt != null ? visitedAt : occurredAt(visited), null);
                addLine(lines, "Next Visit", null, payloadDateTime(visited, "nextVisitAt"), null);
            }

            // "7 days, no repair" 경과일은 클라가 계산
            case LANDLORD_REFUSED -> {
                addLine(lines, "Notice sent", null, sentAt(firstNotice), null);
                addLine(lines, "Follow-up sent", null, sentAt(followUpNotice), null);
            }

            case LANDLORD_BLAME -> {
                CaseEvent blame = findLatestEvent(events, CaseEventType.LANDLORD_BLAME);
                LocalDateTime claimReceivedAt = payloadDateTime(blame, "claimReceivedAt");
                addLine(lines, "Claim received", null, claimReceivedAt != null ? claimReceivedAt : occurredAt(blame), null);
            }

            case REPORTED_311 -> {
                CaseEvent complaint = findLatestEvent(events, CaseEventType.COMPLAINT_311_FILED);
                addLine(lines, "Complaint filed", withHash(payloadString(complaint, "srNumber")), occurredAt(complaint), null);
            }

            case HPD_INSPECTION -> {
                CaseEvent inspection = findLatestEvent(events, CaseEventType.HPD_INSPECTION_SCHEDULED);
                addLine(lines, "Inspection", null, payloadDateTime(inspection, "inspectionStart"), payloadDateTime(inspection, "inspectionEnd"));
            }

            case VIOLATION_UNRESOLVED -> {
                CaseEvent violation = findLatestEvent(events, CaseEventType.VIOLATION_ISSUED);
                addLine(lines, "Violation issued", withHash(payloadString(violation, "violationNumber")), occurredAt(violation), null);
            }

            case SELF_REPAIR -> {
                CaseEvent selfRepair = findLatestEvent(events, CaseEventType.SELF_REPAIR_STARTED);
                addLine(lines, "Repair", null, payloadDateTime(selfRepair, "repairDate"), null);
                addLine(lines, "Landlord notified", null, sentAt(firstNotice), null);
            }

            case CLAIM_SENT -> {
                CaseEvent claim = findLatestEvent(events, CaseEventType.CLAIM_SENT);
                addLine(lines, "Claim sent to landlord", null, occurredAt(claim), null);
            }

            case INSURANCE_CLAIM -> {
                CaseEvent insurance = findLatestEvent(events, CaseEventType.INSURANCE_CLAIM_FILED);
                addLine(lines, "Claim filed with insurance", null, occurredAt(insurance), null);
            }

            case RESOLVED -> {
                LocalDateTime resolvedAt = caseEntity.getResolvedAt();
                if (resolvedAt == null) {
                    resolvedAt = occurredAt(findLatestEvent(events, CaseEventType.RESOLVED));
                }
                addLine(lines, "Resolved", null, resolvedAt, null);
            }

            case RECURRED -> {
                CaseEvent recurred = findLatestEvent(events, CaseEventType.RECURRED);
                LocalDateTime recurredAt = payloadDateTime(recurred, "recurredAt");
                addLine(lines, "Resolved", null, occurredAt(findLatestEvent(events, CaseEventType.RESOLVED)), null);
                addLine(lines, "Recurred", null, recurredAt != null ? recurredAt : occurredAt(recurred), null);
            }
        }

        // DTO 변환
        return CurrentStatusResponse.builder()
                .title(new LocalizedText(template.getTitle(), null))
                .tone(template.getTone())
                .dDay(dDay)
                .lines(lines)
                .startAt(startAt)
                .dueAt(dueAt)
                .primaryCta(StatusCtaResponse.builder()
                        .type(template.getCtaType())
                        .label(template.getCtaLabel())
                        .build())
                .build();
    }

    // ========== 헬퍼 함수 ==========

    // 값이 하나도 없는 라인은 내려주지 않음
    private void addLine(List<StatusLineResponse> lines, String label, String value, LocalDateTime at, LocalDateTime endAt) {
        if (value == null && at == null) {
            return;
        }
        lines.add(StatusLineResponse.builder()
                .label(label)
                .value(value)
                .at(at)
                .endAt(endAt)
                .build());
    }

    // 이벤트는 occurredAt 오름차순으로 들어오므로 뒤에서부터 탐색
    private CaseEvent findLatestEvent(List<CaseEvent> events, CaseEventType type) {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).getType() == type) {
                return events.get(i);
            }
        }
        return null;
    }

    private Notice findLatestNotice(List<Notice> notices, NoticeType type) {
        Notice latest = null;
        for (Notice notice : notices) {
            if (notice.getType() == type && notice.getSentAt() != null
                    && (latest == null || notice.getSentAt().isAfter(latest.getSentAt()))) {
                latest = notice;
            }
        }
        return latest;
    }

    // from → to 사이 뉴욕 날짜 차이
    private long daysBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.DAYS.between(toCaseDate(from), toCaseDate(to));
    }

    private LocalDate toCaseDate(LocalDateTime dateTime) {
        return dateTime.atZone(storageZone).withZoneSameInstant(CASE_ZONE).toLocalDate();
    }

    private String withHash(String number) {
        return number == null ? null : "#" + number;
    }

    private LocalDateTime sentAt(Notice notice) {
        return notice == null ? null : notice.getSentAt();
    }

    private LocalDateTime slaDueAt(Notice notice) {
        return notice == null ? null : notice.getSlaDueAt();
    }

    private LocalDateTime occurredAt(CaseEvent event) {
        return event == null ? null : event.getOccurredAt();
    }

    private LocalDateTime payloadDateTime(CaseEvent event, String key) {
        return event == null ? null : event.getPayloadDateTime(key, storageZone);
    }

    private String payloadString(CaseEvent event, String key) {
        return event == null ? null : event.getPayloadString(key);
    }
}
