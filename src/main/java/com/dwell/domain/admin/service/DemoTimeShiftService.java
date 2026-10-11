package com.dwell.domain.admin.service;

import com.dwell.domain.admin.exception.AdminErrorCode;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventRule;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.cases.service.CaseService;
import com.dwell.domain.cases.service.CaseSlaService;
import com.dwell.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*
데모 시간 이동 (데모 5번 장면: +24시간 미응답 → Escalated)
케이스의 모든 시각을 hours만큼 과거로 옮김 = 그만큼 시간이 흐른 상태 → SLA 체크 즉시 실행 → 갱신된 케이스 상세 반환
dwell.demo.time-shift-enabled=false(기본)면 403 A002 → 운영 서버에서는 끄고 데모 서버 .env에서만 켬
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DemoTimeShiftService {

    // 케이스 이벤트 외 시각 컬럼 (네이티브 일괄 수정, :caseId · :hours)
    private static final List<String> SHIFT_SQLS = List.of(
            "UPDATE cases SET created_at = created_at - make_interval(hours => :hours), modified_at = modified_at - make_interval(hours => :hours), "
                    + "resolved_at = resolved_at - make_interval(hours => :hours), "
                    + "first_observed_at = (first_observed_at + time '12:00' - make_interval(hours => :hours))::date WHERE id = :caseId",
            "UPDATE notice SET created_at = created_at - make_interval(hours => :hours), sent_at = sent_at - make_interval(hours => :hours), "
                    + "sla_due_at = sla_due_at - make_interval(hours => :hours) WHERE case_id = :caseId",
            "UPDATE landlord_response SET created_at = created_at - make_interval(hours => :hours), received_at = received_at - make_interval(hours => :hours), "
                    + "confirmed_at = confirmed_at - make_interval(hours => :hours), "
                    + "repair_date = (repair_date + time '12:00' - make_interval(hours => :hours))::date WHERE case_id = :caseId",
            "UPDATE file SET created_at = created_at - make_interval(hours => :hours), captured_at = captured_at - make_interval(hours => :hours) "
                    + "WHERE evidence_id IN (SELECT id FROM evidence WHERE case_id = :caseId)",
            "UPDATE evidence SET created_at = created_at - make_interval(hours => :hours), analyzed_at = analyzed_at - make_interval(hours => :hours) WHERE case_id = :caseId",
            "UPDATE repair_verification SET created_at = created_at - make_interval(hours => :hours), confirmed_at = confirmed_at - make_interval(hours => :hours) WHERE case_id = :caseId",
            "UPDATE case_checklist_item SET created_at = created_at - make_interval(hours => :hours), modified_at = modified_at - make_interval(hours => :hours) WHERE case_id = :caseId",
            "UPDATE reminder SET created_at = created_at - make_interval(hours => :hours), fire_at = fire_at - make_interval(hours => :hours), "
                    + "fired_at = fired_at - make_interval(hours => :hours), canceled_at = canceled_at - make_interval(hours => :hours) WHERE case_id = :caseId"
    );

    private final EntityManager entityManager;
    private final CaseRepository caseRepository;
    private final CaseEventRepository caseEventRepository;
    private final CaseSlaService caseSlaService;
    private final CaseService caseService;

    @Value("${dwell.demo.time-shift-enabled:false}")
    private boolean timeShiftEnabled;

    @Transactional
    public CaseOverviewResponse shiftCase(Long caseId, int hours) {

        log.info("[DemoTimeShiftService] 데모 시간 이동 - 시작: caseId={}, hours={}", caseId, hours);

        if (!timeShiftEnabled) {
            log.warn("[DemoTimeShiftService] 데모 시간 이동 실패 - 비활성화된 서버 caseId: {}", caseId);
            throw new CustomException(AdminErrorCode.DEMO_DISABLED);
        }

        // 케이스 조회 (삭제된 케이스 제외, 관리자 API라 소유자 검사 X)
        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(caseId).orElseThrow(() -> {
            log.warn("[DemoTimeShiftService] 데모 시간 이동 실패 - 존재하지 않는 케이스 caseId: {}", caseId);
            return new CustomException(CaseErrorCode.CASE_NOT_FOUND);
        });
        Long ownerId = caseEntity.getUser().getId();

        // 케이스 이벤트: 발생 시각 + payload 시각(일정·방문·점검 등)
        for (CaseEvent event : caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity)) {
            event.shiftTimes(CaseEventRule.DATE_TIME_KEYS, hours);
        }
        entityManager.flush();

        // 나머지 시각 컬럼 일괄 수정 후 영속성 컨텍스트의 옛 값 버림
        for (String sql : SHIFT_SQLS) {
            entityManager.createNativeQuery(sql)
                    .setParameter("caseId", caseId)
                    .setParameter("hours", hours)
                    .executeUpdate();
        }
        entityManager.clear();

        // 시간이 흐른 상태로 SLA 체크 즉시 실행
        boolean transitioned = caseSlaService.checkCase(caseId, LocalDateTime.now());

        log.info("[DemoTimeShiftService] 데모 시간 이동 - 종료: caseId={}, transitioned={}", caseId, transitioned);

        // DTO 반환 (갱신된 케이스 상세)
        return caseService.getCaseOverview(caseId, ownerId);
    }
}
