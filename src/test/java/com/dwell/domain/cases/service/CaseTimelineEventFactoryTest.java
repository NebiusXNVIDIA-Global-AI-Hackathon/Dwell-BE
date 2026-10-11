package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.CaseTimelineEventResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventRefType;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeChannel;
import com.dwell.domain.notice.enums.NoticeType;
import com.dwell.global.common.enums.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dwell.domain.cases.service.CaseBannerFactoryTest.newCase;
import static com.dwell.domain.cases.service.CaseBannerFactoryTest.sentNotice;
import static org.assertj.core.api.Assertions.assertThat;

class CaseTimelineEventFactoryTest {

    private final CaseTimelineEventFactory factory = new CaseTimelineEventFactory();

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 9, 28, 20, 12);
    private static final LocalDateTime NOTIFIED = LocalDateTime.of(2026, 9, 28, 20, 30);

    private final Case caseEntity = newCase(CaseStage.AWAITING_RESPONSE);

    @Test
    @DisplayName("와프 예시: 생성 증거는 Case Created에 합치고, 통지 카드 제목은 'Landlord Notified'")
    void wireframeExample() {
        Evidence photo1 = evidence(6L, MediaType.PHOTO, EvidencePhase.INITIAL);
        Evidence photo2 = evidence(7L, MediaType.PHOTO, EvidencePhase.INITIAL);
        Evidence video = evidence(8L, MediaType.VIDEO, EvidencePhase.INITIAL);
        Notice notice = sentNotice(caseEntity, NoticeType.FIRST, NOTIFIED, null);
        ReflectionTestUtils.setField(notice, "id", 7L);
        ReflectionTestUtils.setField(notice, "channel", NoticeChannel.SMS);

        List<CaseEvent> events = List.of(
                event(1L, CaseEventType.CASE_CREATED, CREATED, null, null),
                evidenceAdded(2L, 6L), evidenceAdded(3L, 7L), evidenceAdded(4L, 8L),
                event(5L, CaseEventType.NOTICE_SENT, NOTIFIED, 7L, Map.of("recipientName", "John Smith")));

        List<CaseTimelineEventResponse> cards = factory.create(events, List.of(photo1, photo2, video), List.of(), List.of(notice));

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).getTitle().en()).isEqualTo("Case Created");
        assertThat(cards.get(0).getDescription().en()).isEqualTo("You documented the issue with 2 photos and 1 video.");
        assertThat(cards.get(0).getEvidenceIds()).containsExactly(6L, 7L, 8L);
        assertThat(cards.get(1).getTitle().en()).isEqualTo("Landlord Notified");
        assertThat(cards.get(1).getDescription().en()).isEqualTo("Sent by text to John Smith (Landlord).");
        assertThat(cards.get(1).getRefId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("증거 1건에 파일 여러 개면 파일 단위로 셈 (시드: MULTI 증거 + 사진 2·영상 1)")
    void countsFiles() {
        Evidence multi = evidence(6L, MediaType.MULTI, EvidencePhase.INITIAL);
        List<File> files = List.of(file(multi, MediaType.PHOTO), file(multi, MediaType.PHOTO), file(multi, MediaType.VIDEO));
        List<CaseEvent> events = List.of(event(1L, CaseEventType.CASE_CREATED, CREATED, null, null), evidenceAdded(2L, 6L));

        List<CaseTimelineEventResponse> cards = factory.create(events, List.of(multi), files, List.of());

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getDescription().en()).isEqualTo("You documented the issue with 2 photos and 1 video.");
        assertThat(cards.get(0).getEvidenceIds()).containsExactly(6L);
    }

    @Test
    @DisplayName("생성 카드 개수는 payload 기록 값 우선, 통지 값이 없으면 'to your landlord'")
    void snapshotFirstAndFallback() {
        List<CaseEvent> events = List.of(
                event(1L, CaseEventType.CASE_CREATED, CREATED, null, Map.of("photoCount", 1, "videoCount", 0)),
                event(2L, CaseEventType.NOTICE_SENT, NOTIFIED, null, null));

        List<CaseTimelineEventResponse> cards = factory.create(events, List.of(), List.of(), List.of());

        assertThat(cards.get(0).getDescription().en()).isEqualTo("You documented the issue with 1 photo.");
        assertThat(cards.get(1).getDescription().en()).isEqualTo("Sent to your landlord.");
    }

    @Test
    @DisplayName("나중에 추가한 증거는 카드 따로, 삭제된 증거 이벤트는 숨김")
    void laterEvidence() {
        Evidence after = evidence(9L, MediaType.PHOTO, EvidencePhase.AFTER_REPAIR);
        List<CaseEvent> events = List.of(
                event(1L, CaseEventType.CASE_CREATED, CREATED, null, null),
                evidenceAdded(2L, 9L),
                evidenceAdded(3L, 99L)); // 삭제된 증거

        List<CaseTimelineEventResponse> cards = factory.create(events, List.of(after), List.of(), List.of());

        assertThat(cards).extracting(card -> card.getType()).containsExactly(CaseEventType.CASE_CREATED, CaseEventType.EVIDENCE_ADDED);
        assertThat(cards.get(0).getDescription().en()).isEqualTo("You documented the issue.");
        assertThat(cards.get(1).getDescription().en()).isEqualTo("You added 1 photo after the repair.");
        assertThat(cards.get(1).getEvidenceIds()).containsExactly(9L);
    }

    @Test
    @DisplayName("일정 확정: 같은 날 범위, 업체, 뉴욕 시각 (오프셋 변환), meta에서 note 제외")
    void repairScheduled() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scheduledStart", "2026-10-14T14:00:00Z"); // 뉴욕 10:00
        payload.put("scheduledEnd", "2026-10-14T12:00:00-04:00");
        payload.put("repairedBy", "ABC Plumbing");
        payload.put("note", "texted");

        CaseTimelineEventResponse card = single(event(1L, CaseEventType.REPAIR_SCHEDULED, NOTIFIED, null, payload));

        assertThat(card.getDescription().en()).isEqualTo("Repair scheduled for Oct 14, 10:00 AM – 12:00 PM by ABC Plumbing.");
        assertThat(card.getMeta()).containsOnlyKeys("scheduledStart", "scheduledEnd", "repairedBy");
    }

    @Test
    @DisplayName("방문: 제목 1st/2nd Visit, 다음 방문이 없으면 그 문장 생략")
    void visits() {
        List<CaseEvent> events = List.of(
                event(1L, CaseEventType.REPAIR_VISITED, CREATED, null, Map.of("visitedAt", "2026-10-12T09:00:00", "nextVisitAt", "2026-10-14")),
                event(2L, CaseEventType.REPAIR_VISITED, NOTIFIED, null, Map.of("visitedAt", "2026-10-14T09:00:00")));

        List<CaseTimelineEventResponse> cards = factory.create(events, List.of(), List.of(), List.of());

        assertThat(cards).extracting(card -> card.getTitle().en()).containsExactly("1st Visit", "2nd Visit");
        assertThat(cards.get(0).getDescription().en()).isEqualTo("Repair visit on Oct 12. Next visit Oct 14.");
        assertThat(cards.get(1).getDescription().en()).isEqualTo("Repair visit on Oct 14.");
    }

    @Test
    @DisplayName("311·위반·청구·보험·재발 문장, 값이 없으면 그 부분만 생략")
    void otherTemplates() {
        assertThat(description(CaseEventType.COMPLAINT_311_FILED, Map.of("srNumber", "311-12345678"))).isEqualTo("Filed a 311 complaint. SR #311-12345678.");
        assertThat(description(CaseEventType.VIOLATION_ISSUED, Map.of("violationNumber", "12345678", "violationClass", "C"))).isEqualTo("HPD issued a violation #12345678 (Class C).");
        assertThat(description(CaseEventType.VIOLATION_ISSUED, Map.of("violationNumber", "12345678"))).isEqualTo("HPD issued a violation #12345678.");
        assertThat(description(CaseEventType.CLAIM_SENT, Map.of("amount", 480, "dueDate", "2026-11-01"))).isEqualTo("Sent a repair cost claim of $480 to your landlord. Due Nov 1.");
        assertThat(description(CaseEventType.CLAIM_SENT, Map.of("amount", "1250.5"))).isEqualTo("Sent a repair cost claim of $1,250.50 to your landlord.");
        assertThat(description(CaseEventType.CLAIM_SENT, null)).isEqualTo("Sent a repair cost claim to your landlord.");
        assertThat(description(CaseEventType.INSURANCE_CLAIM_FILED, Map.of("claimNumber", "INS-1"))).isEqualTo("Filed an insurance claim #INS-1.");
        assertThat(description(CaseEventType.HPD_INSPECTION_SCHEDULED, Map.of("inspectionStart", "2026-10-14T10:00:00"))).isEqualTo("HPD inspection scheduled for Oct 14, 10:00 AM.");
        assertThat(description(CaseEventType.RECURRED, null)).isEqualTo("The problem came back.");
    }

    @Test
    @DisplayName("모든 이벤트 타입에 제목이 있고, Case Created 외에는 설명도 있음 (스케줄러·검증 이벤트 포함)")
    void allTypesHaveText() {
        for (CaseEventType type : CaseEventType.values()) {
            if (type == CaseEventType.EVIDENCE_ADDED) {
                continue;
            }
            CaseTimelineEventResponse card = single(event(1L, type, CREATED, null, null));
            assertThat(card.getTitle().en()).as(type.name()).isNotBlank();
            assertThat(card.getDescription()).as(type.name()).isNotNull();
        }
    }

    // ========== 테스트 픽스처 ==========

    private String description(CaseEventType type, Map<String, Object> payload) {
        return single(event(1L, type, CREATED, null, payload)).getDescription().en();
    }

    private CaseTimelineEventResponse single(CaseEvent event) {
        List<CaseTimelineEventResponse> cards = factory.create(List.of(event), List.of(), List.of(), List.of());
        assertThat(cards).hasSize(1);
        return cards.get(0);
    }

    private CaseEvent event(Long id, CaseEventType type, LocalDateTime occurredAt, Long refId, Map<String, Object> payload) {
        CaseEvent event = CaseEvent.builder()
                .caseEntity(caseEntity)
                .type(type)
                .occurredAt(occurredAt)
                .refId(refId)
                .payload(payload)
                .build();
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private CaseEvent evidenceAdded(Long id, Long evidenceId) {
        CaseEvent event = CaseEvent.builder()
                .caseEntity(caseEntity)
                .type(CaseEventType.EVIDENCE_ADDED)
                .occurredAt(CREATED)
                .refType(CaseEventRefType.EVIDENCE)
                .refId(evidenceId)
                .build();
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private File file(Evidence evidence, MediaType mediaType) {
        File file = File.builder().mediaType(mediaType).build();
        ReflectionTestUtils.setField(file, "evidence", evidence);
        return file;
    }

    private Evidence evidence(Long id, MediaType mediaType, EvidencePhase phase) {
        Evidence evidence = Evidence.builder()
                .caseEntity(caseEntity)
                .mediaType(mediaType)
                .phase(phase)
                .build();
        ReflectionTestUtils.setField(evidence, "id", id);
        return evidence;
    }
}
