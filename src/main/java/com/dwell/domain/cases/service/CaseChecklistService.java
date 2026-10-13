package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.request.ChecklistItemCreateRequest;
import com.dwell.domain.cases.dto.request.ChecklistItemUpdateRequest;
import com.dwell.domain.cases.dto.response.ChecklistItemResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import com.dwell.domain.cases.enums.ChecklistSource;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
케이스 체크리스트 항목 추가 · 수정(체크/문구) · 삭제
AI 항목도 문구 수정·삭제 가능 (문구가 바뀌면 번역본은 비움)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CaseChecklistService {

    private final CaseChecklistItemRepository caseChecklistItemRepository;
    private final CaseService caseService;

    @Transactional
    public ChecklistItemResponse addItem(Long caseId, Long userId, ChecklistItemCreateRequest request) {

        log.info("[CaseChecklistService] 체크리스트 항목 추가 - 시작: caseId={}", caseId);

        // 앞뒤 공백 제거 후 빈 문구 검증
        String text = trimText(request.getText());
        if (text.isEmpty()) {
            log.warn("[CaseChecklistService] 체크리스트 항목 추가 실패 - 빈 문구 caseId: {}", caseId);
            throw new CustomException(CaseErrorCode.INVALID_CHECKLIST_TEXT);
        }

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseService.findOwnedCase(caseId, userId);

        // 맨 아래에 추가 (항목이 없으면 1부터)
        int sortOrder = caseChecklistItemRepository.findTopByCaseEntityOrderBySortOrderDesc(caseEntity)
                .map(item -> item.getSortOrder() + 1)
                .orElse(1);

        CaseChecklistItem item = caseChecklistItemRepository.save(CaseChecklistItem.builder()
                .caseEntity(caseEntity)
                .text(text)
                .textNative(null)
                .source(ChecklistSource.USER)
                .sortOrder(sortOrder)
                .build());

        log.info("[CaseChecklistService] 체크리스트 항목 추가 - 종료: caseId={}, itemId={}, sortOrder={}", caseId, item.getId(), sortOrder);

        // DTO 반환
        return toResponse(item);
    }

    @Transactional
    public ChecklistItemResponse updateItem(Long caseId, Long itemId, Long userId, ChecklistItemUpdateRequest request) {

        log.info("[CaseChecklistService] 체크리스트 항목 수정 - 시작: caseId={}, itemId={}", caseId, itemId);

        // 수정할 값이 하나도 없으면 400 (요청 값 검증을 케이스·항목 조회보다 먼저)
        if (request.getChecked() == null && request.getText() == null) {
            log.warn("[CaseChecklistService] 체크리스트 항목 수정 실패 - 수정할 값 없음 caseId: {}, itemId: {}", caseId, itemId);
            throw new CustomException(CaseErrorCode.EMPTY_CHECKLIST_UPDATE);
        }

        // 문구 검증 (앞뒤 공백 제거 후 빈 문구면 400)
        String text = null;
        if (request.getText() != null) {
            text = trimText(request.getText());
            if (text.isEmpty()) {
                log.warn("[CaseChecklistService] 체크리스트 항목 수정 실패 - 빈 문구 caseId: {}, itemId: {}", caseId, itemId);
                throw new CustomException(CaseErrorCode.INVALID_CHECKLIST_TEXT);
            }
        }

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseService.findOwnedCase(caseId, userId);

        // 이 케이스의 항목 조회
        CaseChecklistItem item = findItem(caseEntity, caseId, itemId);

        // 보낸 값만 반영
        if (request.getChecked() != null) {
            item.changeChecked(request.getChecked());
        }
        if (text != null) {
            item.changeText(text);
        }

        log.info("[CaseChecklistService] 체크리스트 항목 수정 - 종료: caseId={}, itemId={}", caseId, itemId);

        // DTO 반환
        return toResponse(item);
    }

    @Transactional
    public void deleteItem(Long caseId, Long itemId, Long userId) {

        log.info("[CaseChecklistService] 체크리스트 항목 삭제 - 시작: caseId={}, itemId={}", caseId, itemId);

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseService.findOwnedCase(caseId, userId);

        // 이 케이스의 항목 조회 후 영구 삭제 (source 무관)
        CaseChecklistItem item = findItem(caseEntity, caseId, itemId);
        caseChecklistItemRepository.delete(item);

        log.info("[CaseChecklistService] 체크리스트 항목 삭제 - 종료: caseId={}, itemId={}", caseId, itemId);
    }

    // ========== 헬퍼 함수 ==========

    // 이 케이스의 항목 조회 (없거나 다른 케이스의 항목이면 404)
    private CaseChecklistItem findItem(Case caseEntity, Long caseId, Long itemId) {
        return caseChecklistItemRepository.findByIdAndCaseEntity(itemId, caseEntity).orElseThrow(() -> {
            log.warn("[CaseChecklistService] 체크리스트 항목 조회 실패 - 항목 없음 caseId: {}, itemId: {}", caseId, itemId);
            return new CustomException(CaseErrorCode.CHECKLIST_ITEM_NOT_FOUND);
        });
    }

    // 앞뒤 공백 제거 (null이면 빈 문자열)
    private String trimText(String text) {
        return text == null ? "" : text.trim();
    }

    // DTO 변환
    private ChecklistItemResponse toResponse(CaseChecklistItem item) {
        return ChecklistItemResponse.builder()
                .itemId(item.getId())
                .text(new LocalizedText(item.getText(), item.getTextNative()))
                .checked(item.isChecked())
                .source(item.getSource())
                .build();
    }
}
