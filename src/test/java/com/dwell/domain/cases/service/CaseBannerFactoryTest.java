package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.response.CurrentStatusResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.BannerCtaType;
import com.dwell.domain.cases.enums.BannerTone;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseBannerFactoryTest {

    // 테스트 시각은 UTC로 저장됐다고 가정
    private final CaseBannerFactory factory = new CaseBannerFactory(ZoneOffset.UTC);

    // 2026-09-29 12:00 UTC = 뉴욕 08:00
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);

    @ParameterizedTest
    @EnumSource(CaseStage.class)
    @DisplayName("19개 stage 모두 데이터가 없어도 제목·색·버튼이 나온다")
    void allStagesHaveBanner(CaseStage stage) {
        CurrentStatusResponse banner = factory.create(newCase(stage), List.of(), List.of(), NOW);

        assertThat(banner.getTitle().en()).isNotBlank();
        assertThat(banner.getTone()).isNotNull();
        assertThat(banner.getPrimaryCta().getType()).isNotNull();
        assertThat(banner.getPrimaryCta().getLabel()).isNotBlank();
        assertThat(banner.getLines()).isEmpty();
    }

    @Test
    @DisplayName("AWAITING_RESPONSE: 통지 발송·응답 기한 라인과 진행바 시작/기한")
    void awaitingResponse() {
        Case caseEntity = newCase(CaseStage.AWAITING_RESPONSE);
        Notice notice = sentNotice(caseEntity, NoticeType.FIRST, LocalDateTime.of(2026, 9, 28, 19, 0), LocalDateTime.of(2026, 9, 29, 19, 0));

        CurrentStatusResponse banner = factory.create(caseEntity, List.of(), List.of(notice), NOW);

        assertThat(banner.getTitle().en()).isEqualTo("Waiting for your landlord's response");
        assertThat(banner.getPrimaryCta().getType()).isEqualTo(BannerCtaType.UPDATE_STATUS);
        assertThat(banner.getPrimaryCta().getLabel()).isEqualTo("Got a Reply? Update Status");
        assertThat(banner.getLines()).extracting("label").containsExactly("Notice sent", "Response due");
        assertThat(banner.getStartAt()).isEqualTo(notice.getSentAt());
        assertThat(banner.getDueAt()).isEqualTo(notice.getSlaDueAt());
        assertThat(banner.getDDay()).isNull();
    }

    @Test
    @DisplayName("RESPONSE_OVERDUE: 기한 다음날이면 D+1")
    void responseOverdueDPlusOne() {
        Case caseEntity = newCase(CaseStage.RESPONSE_OVERDUE);
        Notice notice = sentNotice(caseEntity, NoticeType.FIRST, LocalDateTime.of(2026, 9, 27, 19, 0), LocalDateTime.of(2026, 9, 28, 19, 0));

        CurrentStatusResponse banner = factory.create(caseEntity, List.of(), List.of(notice), NOW);

        assertThat(banner.getDDay()).isEqualTo(1);
        assertThat(banner.getLines()).extracting("label").containsExactly("Notice sent");
    }

    @Test
    @DisplayName("FOLLOW_UP_SENT: 1차가 아닌 재통지 기준으로 라인을 만든다")
    void followUpSentUsesFollowUpNotice() {
        Case caseEntity = newCase(CaseStage.FOLLOW_UP_SENT);
        Notice first = sentNotice(caseEntity, NoticeType.FIRST, LocalDateTime.of(2026, 9, 27, 19, 0), LocalDateTime.of(2026, 9, 28, 19, 0));
        Notice followUp = sentNotice(caseEntity, NoticeType.FOLLOW_UP, LocalDateTime.of(2026, 9, 29, 10, 0), LocalDateTime.of(2026, 9, 30, 10, 0));

        CurrentStatusResponse banner = factory.create(caseEntity, List.of(), List.of(first, followUp), NOW);

        assertThat(banner.getLines()).extracting("label").containsExactly("Follow-up sent", "Response due");
        assertThat(banner.getStartAt()).isEqualTo(followUp.getSentAt());
        assertThat(banner.getDueAt()).isEqualTo(followUp.getSlaDueAt());
    }

    @Test
    @DisplayName("REPAIR_SCHEDULED: 수리 3일 전이면 D-3, 시간대 라인")
    void repairScheduledDMinusThree() {
        Case caseEntity = newCase(CaseStage.REPAIR_SCHEDULED);
        CaseEvent scheduled = event(caseEntity, CaseEventType.REPAIR_SCHEDULED, LocalDateTime.of(2026, 9, 28, 14, 0),
                Map.of("scheduledStart", "2026-10-02T10:00:00-04:00", "scheduledEnd", "2026-10-02T12:00:00-04:00"));

        CurrentStatusResponse banner = factory.create(caseEntity, List.of(scheduled), List.of(), NOW);

        assertThat(banner.getDDay()).isEqualTo(-3);
        assertThat(banner.getLines().get(0).getAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 14, 0));
        assertThat(banner.getLines().get(0).getEndAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 16, 0));
    }

    @Test
    @DisplayName("REPAIR_SCHEDULED: 오프셋 없는 시각·날짜만 있는 payload는 뉴욕 현지 시각으로 해석")
    void repairScheduledPayloadWithoutOffsetIsNewYorkTime() {
        Case caseEntity = newCase(CaseStage.REPAIR_SCHEDULED);
        CaseEvent withoutOffset = event(caseEntity, CaseEventType.REPAIR_SCHEDULED, LocalDateTime.of(2026, 9, 28, 14, 0),
                Map.of("scheduledStart", "2026-10-02T10:00:00"));
        CaseEvent dateOnly = event(caseEntity, CaseEventType.REPAIR_SCHEDULED, LocalDateTime.of(2026, 9, 28, 14, 0),
                Map.of("scheduledStart", "2026-10-02"));

        CurrentStatusResponse withoutOffsetBanner = factory.create(caseEntity, List.of(withoutOffset), List.of(), NOW);
        CurrentStatusResponse dateOnlyBanner = factory.create(caseEntity, List.of(dateOnly), List.of(), NOW);

        // 뉴욕 10:00 = UTC 14:00, 뉴욕 자정 = UTC 04:00 → 둘 다 뉴욕 10/2 기준 D-3
        assertThat(withoutOffsetBanner.getLines().get(0).getAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 14, 0));
        assertThat(withoutOffsetBanner.getDDay()).isEqualTo(-3);
        assertThat(dateOnlyBanner.getLines().get(0).getAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 4, 0));
        assertThat(dateOnlyBanner.getDDay()).isEqualTo(-3);
    }

    @Test
    @DisplayName("REPORTED_311: 빨간 배너 + SR 번호 값")
    void reported311() {
        Case caseEntity = newCase(CaseStage.REPORTED_311);
        CaseEvent complaint = event(caseEntity, CaseEventType.COMPLAINT_311_FILED, LocalDateTime.of(2026, 10, 5, 13, 0),
                Map.of("srNumber", "311-12345678"));

        CurrentStatusResponse banner = factory.create(caseEntity, List.of(complaint), List.of(), NOW);

        assertThat(banner.getTone()).isEqualTo(BannerTone.DANGER);
        assertThat(banner.getPrimaryCta().getType()).isEqualTo(BannerCtaType.TALK_TO_DEAVER);
        assertThat(banner.getLines().get(0).getValue()).isEqualTo("#311-12345678");
        assertThat(banner.getLines().get(0).getAt()).isEqualTo(complaint.getOccurredAt());
    }

    @Test
    @DisplayName("RESOLVED는 초록, RECURRED는 보라")
    void resolvedAndRecurredTone() {
        assertThat(factory.create(newCase(CaseStage.RESOLVED), List.of(), List.of(), NOW).getTone()).isEqualTo(BannerTone.SUCCESS);
        assertThat(factory.create(newCase(CaseStage.RECURRED), List.of(), List.of(), NOW).getTone()).isEqualTo(BannerTone.RECURRED);
    }

    // ========== 테스트 픽스처 ==========

    static Case newCase(CaseStage stage) {
        Case caseEntity = Case.builder()
                .caseNumber("C-006")
                .title("Water Leak in Bathroom · Ceiling")
                .stage(stage)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
        return caseEntity;
    }

    static CaseEvent event(Case caseEntity, CaseEventType type, LocalDateTime occurredAt, Map<String, Object> payload) {
        return CaseEvent.builder()
                .caseEntity(caseEntity)
                .type(type)
                .occurredAt(occurredAt)
                .payload(payload)
                .build();
    }

    static Notice sentNotice(Case caseEntity, NoticeType type, LocalDateTime sentAt, LocalDateTime slaDueAt) {
        Notice notice = Notice.builder()
                .caseEntity(caseEntity)
                .type(type)
                .build();
        ReflectionTestUtils.setField(notice, "sentAt", sentAt);
        ReflectionTestUtils.setField(notice, "slaDueAt", slaDueAt);
        return notice;
    }
}
