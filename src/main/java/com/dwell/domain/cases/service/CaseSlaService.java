package com.dwell.domain.cases.service;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.enums.NoticeType;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.global.common.LocalizedText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/*
SLA 체크 (서버 전용 전이, 케이스 1건 단위)
- 1차 통지 기한 경과: AWAITING_RESPONSE → RESPONSE_OVERDUE (+ 경고 이벤트)
- 2차 통지 기한 경과: FOLLOW_UP_SENT → FOLLOW_UP_OVERDUE (+ 경고 이벤트)
- 수리 예정일(뉴욕 날짜) 도래: REPAIR_SCHEDULED → REPAIR_DAY (이벤트 없음, 전이 후보 P12 보류)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CaseSlaService {

    // 기한이 걸린 stage (SLA 체크 대상)
    public static final List<CaseStage> TARGET_STAGES = List.of(
            CaseStage.AWAITING_RESPONSE, CaseStage.FOLLOW_UP_SENT, CaseStage.REPAIR_SCHEDULED);

    // 수리 예정일은 뉴욕 날짜 기준
    private static final ZoneId CASE_ZONE = ZoneId.of("America/New_York");

    private final CaseRepository caseRepository;
    private final CaseEventRepository caseEventRepository;
    private final NoticeRepository noticeRepository;

    // SLA 체크 대상 케이스 ID
    public List<Long> findTargetCaseIds() {
        return caseRepository.findAllByStageInAndDeletedAtIsNull(TARGET_STAGES).stream()
                .map(Case::getId)
                .toList();
    }

    // 케이스 1건 SLA 체크 - 전이했으면 true (now = DB 시각과 같은 JVM 기본 시간대)
    @Transactional
    public boolean checkCase(Long caseId, LocalDateTime now) {

        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(caseId).orElse(null);
        if (caseEntity == null) {
            return false;
        }

        CaseStage fromStage = caseEntity.getStage();
        boolean transitioned = switch (fromStage) {
            case AWAITING_RESPONSE -> overdue(caseEntity, NoticeType.FIRST, CaseStage.RESPONSE_OVERDUE, CaseEventType.RESPONSE_OVERDUE, now);
            case FOLLOW_UP_SENT -> overdue(caseEntity, NoticeType.FOLLOW_UP, CaseStage.FOLLOW_UP_OVERDUE, CaseEventType.FOLLOW_UP_OVERDUE, now);
            case REPAIR_SCHEDULED -> repairDay(caseEntity, now);
            default -> false;
        };

        if (transitioned) {
            log.info("[CaseSlaService] SLA 전이: caseId={}, {} -> {}", caseId, fromStage, caseEntity.getStage());
        }
        return transitioned;
    }

    // ========== 헬퍼 함수 ==========

    // 통지 응답 기한이 지났으면 전이 + 경고 이벤트 (발생 시각 = 기한, 마지막 이벤트보다 앞서면 그 시각)
    private boolean overdue(Case caseEntity, NoticeType noticeType, CaseStage toStage, CaseEventType eventType, LocalDateTime now) {

        Notice notice = findLatestSentNotice(caseEntity, noticeType);
        if (notice == null || notice.getSlaDueAt() == null || notice.getSlaDueAt().isAfter(now)) {
            return false;
        }

        LocalDateTime occurredAt = notice.getSlaDueAt().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime lastOccurredAt = caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(caseEntity)
                .map(CaseEvent::getOccurredAt)
                .orElse(null);
        if (lastOccurredAt != null && occurredAt.isBefore(lastOccurredAt)) {
            occurredAt = lastOccurredAt;
        }

        caseEventRepository.save(CaseEvent.builder()
                .caseEntity(caseEntity)
                .type(eventType)
                .occurredAt(occurredAt)
                .title(new LocalizedText(eventType.getLabel(), null))
                .alert(true)
                .build());
        caseEntity.changeStage(toStage);
        return true;
    }

    // 수리 예정일(뉴욕 날짜)이 오늘이거나 지났으면 REPAIR_DAY
    private boolean repairDay(Case caseEntity, LocalDateTime now) {

        CaseEvent scheduled = caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity).stream()
                .filter(event -> event.getType() == CaseEventType.REPAIR_SCHEDULED)
                .reduce((first, second) -> second)
                .orElse(null);
        LocalDateTime scheduledStart = scheduled == null ? null : scheduled.getPayloadDateTime("scheduledStart", ZoneId.systemDefault());
        if (scheduledStart == null || toCaseDate(scheduledStart).isAfter(toCaseDate(now))) {
            return false;
        }

        caseEntity.changeStage(CaseStage.REPAIR_DAY);
        return true;
    }

    private Notice findLatestSentNotice(Case caseEntity, NoticeType type) {
        Notice latest = null;
        for (Notice notice : noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity)) {
            if (notice.getType() == type) {
                latest = notice;
            }
        }
        return latest;
    }

    private LocalDate toCaseDate(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(CASE_ZONE).toLocalDate();
    }
}
