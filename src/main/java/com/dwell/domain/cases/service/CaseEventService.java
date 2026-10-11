package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.request.CaseEventCreateRequest;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventRule;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/*
케이스 상태 업데이트 (이벤트 기록 + stage 전이)
POST /cases/{caseId}/events와 Assistant 채팅 액션이 같은 메소드를 사용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CaseEventService {

    // 서버·기기 시계 차이 허용 범위 (이보다 미래면 400)
    private static final long FUTURE_TOLERANCE_MINUTES = 5;

    private final CaseEventRepository caseEventRepository;
    private final CaseService caseService;

    @Transactional
    public CaseOverviewResponse recordEvent(Long caseId, Long userId, CaseEventCreateRequest request) {

        log.info("[CaseEventService] 케이스 이벤트 기록 - 시작: caseId={}, type={}", caseId, request.getType());

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseService.findOwnedCase(caseId, userId);

        // 클라이언트가 기록할 수 있는 이벤트인지 (스케줄러·통지 발송·검증 API 전용 이벤트 X)
        CaseEventRule rule = CaseEventRule.of(request.getType()).orElseThrow(() -> {
            log.warn("[CaseEventService] 이벤트 기록 실패 - 클라이언트 기록 불가 이벤트 caseId: {}, type: {}", caseId, request.getType());
            return new CustomException(CaseErrorCode.EVENT_TYPE_NOT_ALLOWED);
        });

        // 요청 값 검증 (400) 후 상태 검증 (409)
        Map<String, Object> payload = toPayload(rule, request);
        LocalDateTime occurredAt = toOccurredAt(caseEntity, request.getOccurredAt());

        // 현재 stage에서 가능한 전이인지
        CaseStage fromStage = caseEntity.getStage();
        if (!rule.canTransitFrom(fromStage)) {
            log.warn("[CaseEventService] 이벤트 기록 실패 - 허용되지 않는 전이 caseId: {}, stage: {}, type: {}", caseId, fromStage, request.getType());
            throw new CustomException(CaseErrorCode.INVALID_TRANSITION);
        }

        // 이벤트 기록
        caseEventRepository.save(CaseEvent.builder()
                .caseEntity(caseEntity)
                .type(rule.getType())
                .occurredAt(occurredAt)
                .title(new LocalizedText(rule.getType().getLabel(), null))
                .payload(payload)
                .alert(rule.isAlert())
                .build());

        // stage 전이
        CaseStage toStage = rule.nextStage(fromStage, payload);
        caseEntity.changeStage(toStage);

        log.info("[CaseEventService] 케이스 이벤트 기록 - 종료: caseId={}, {} -> {}", caseId, fromStage, toStage);

        // DTO 반환 (갱신된 케이스 상세)
        return caseService.getCaseOverview(caseId, userId);
    }

    // ========== 헬퍼 함수 ==========

    // 필수 키 확인 + 시각 형식 확인 + 규칙에 있는 키만 저장 (note는 payload에 함께 저장)
    private Map<String, Object> toPayload(CaseEventRule rule, CaseEventCreateRequest request) {

        Map<String, Object> source = request.getPayload() == null ? Map.of() : request.getPayload();

        for (String key : rule.getRequiredKeys()) {
            Object value = source.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                log.warn("[CaseEventService] 이벤트 기록 실패 - 필수 payload 누락 type: {}, key: {}", rule.getType(), key);
                throw new CustomException(CaseErrorCode.INVALID_PAYLOAD);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        for (String key : Stream.concat(rule.getRequiredKeys().stream(), rule.getOptionalKeys().stream()).toList()) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            // 값은 문자열·숫자·불린만 (객체·배열 X)
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                log.warn("[CaseEventService] 이벤트 기록 실패 - payload 값 형식 오류 type: {}, key: {}", rule.getType(), key);
                throw new CustomException(CaseErrorCode.INVALID_PAYLOAD);
            }
            if (CaseEventRule.DATE_TIME_KEYS.contains(key) && CaseEvent.parseDateTime(String.valueOf(value), ZoneId.systemDefault()) == null) {
                log.warn("[CaseEventService] 이벤트 기록 실패 - payload 시각 형식 오류 type: {}, key: {}", rule.getType(), key);
                throw new CustomException(CaseErrorCode.INVALID_PAYLOAD);
            }
            payload.put(key, value);
        }

        if (request.getNote() != null && !request.getNote().isBlank()) {
            payload.put("note", request.getNote());
        }
        return payload;
    }

    // 발생 시각: 생략하면 현재(초 단위), 형식 오류·미래·마지막 이벤트보다 이전이면 400 (DB 시각과 같은 JVM 기본 시간대)
    // 마지막 이벤트보다 이전을 막는 이유: 진행 스텝이 시각순이라 순서가 뒤집힘
    private LocalDateTime toOccurredAt(Case caseEntity, String value) {

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime lastOccurredAt = caseEventRepository.findTopByCaseEntityOrderByOccurredAtDescIdDesc(caseEntity)
                .map(CaseEvent::getOccurredAt)
                .orElse(null);

        // 생략: 현재 시각. 마지막 이벤트가 현재보다 뒤(허용 오차 안의 미래)면 그 시각으로 맞춰 순서 유지
        if (value == null || value.isBlank()) {
            return lastOccurredAt != null && now.isBefore(lastOccurredAt) ? lastOccurredAt : now;
        }

        LocalDateTime occurredAt = CaseEvent.parseDateTime(value, ZoneId.systemDefault());

        if (occurredAt == null
                || occurredAt.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))
                || (lastOccurredAt != null && occurredAt.isBefore(lastOccurredAt))) {
            log.warn("[CaseEventService] 이벤트 기록 실패 - 잘못된 발생 시각 occurredAt: {}, lastOccurredAt: {}", value, lastOccurredAt);
            throw new CustomException(CaseErrorCode.INVALID_OCCURRED_AT);
        }
        return occurredAt;
    }
}
