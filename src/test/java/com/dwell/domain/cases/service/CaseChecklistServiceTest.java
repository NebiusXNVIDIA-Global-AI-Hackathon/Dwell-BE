package com.dwell.domain.cases.service;

import com.dwell.domain.cases.dto.request.ChecklistItemCreateRequest;
import com.dwell.domain.cases.dto.request.ChecklistItemUpdateRequest;
import com.dwell.domain.cases.dto.response.ChecklistItemResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.ChecklistSource;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseChecklistServiceTest {

    @Mock
    private CaseChecklistItemRepository caseChecklistItemRepository;
    @Mock
    private CaseService caseService;

    private CaseChecklistService caseChecklistService;

    @BeforeEach
    void setUp() {
        caseChecklistService = new CaseChecklistService(caseChecklistItemRepository, caseService);
    }

    // ========== 추가 ==========

    @Test
    @DisplayName("추가: sortOrder는 마지막 항목 + 1, 앞뒤 공백 제거, source USER, native null, checked false")
    void addItem() {
        Case caseEntity = caseOf();
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseChecklistItemRepository.findTopByCaseEntityOrderBySortOrderDesc(caseEntity))
                .willReturn(Optional.of(itemOf(caseEntity, 31L, "Take photos", "사진 찍기", ChecklistSource.AI, 4)));
        given(caseChecklistItemRepository.save(any(CaseChecklistItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        ChecklistItemResponse response = caseChecklistService.addItem(6L, 1L, createRequest("  Ask neighbor on 5F  "));

        ArgumentCaptor<CaseChecklistItem> saved = ArgumentCaptor.forClass(CaseChecklistItem.class);
        verify(caseChecklistItemRepository).save(saved.capture());
        assertThat(saved.getValue().getSortOrder()).isEqualTo(5);
        assertThat(saved.getValue().getText()).isEqualTo("Ask neighbor on 5F");
        assertThat(saved.getValue().getSource()).isEqualTo(ChecklistSource.USER);
        assertThat(saved.getValue().getTextNative()).isNull();
        assertThat(saved.getValue().isChecked()).isFalse();
        assertThat(response.getText().en()).isEqualTo("Ask neighbor on 5F");
        assertThat(response.getText().nativeText()).isNull();
        assertThat(response.getSource()).isEqualTo(ChecklistSource.USER);
    }

    @Test
    @DisplayName("추가: 항목이 없으면 sortOrder 1")
    void addItemFirst() {
        Case caseEntity = caseOf();
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseChecklistItemRepository.findTopByCaseEntityOrderBySortOrderDesc(caseEntity)).willReturn(Optional.empty());
        given(caseChecklistItemRepository.save(any(CaseChecklistItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        caseChecklistService.addItem(6L, 1L, createRequest("First"));

        ArgumentCaptor<CaseChecklistItem> saved = ArgumentCaptor.forClass(CaseChecklistItem.class);
        verify(caseChecklistItemRepository).save(saved.capture());
        assertThat(saved.getValue().getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("추가: 공백만 있는 문구는 INVALID_CHECKLIST_TEXT (케이스 조회 전에 검증)")
    void addItemBlank() {
        assertThatThrownBy(() -> caseChecklistService.addItem(6L, 1L, createRequest("   ")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_CHECKLIST_TEXT);
        verify(caseService, never()).findOwnedCase(any(), any());
        verify(caseChecklistItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("추가: 케이스 조회 실패(403)는 그대로 전파")
    void addItemCaseFailure() {
        given(caseService.findOwnedCase(6L, 1L)).willThrow(new CustomException(CaseErrorCode.NOT_CASE_OWNER));

        assertThatThrownBy(() -> caseChecklistService.addItem(6L, 1L, createRequest("x")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
        verify(caseChecklistItemRepository, never()).save(any());
    }

    // ========== 수정 ==========

    @Test
    @DisplayName("수정: checked만 보내면 체크 여부만 바뀌고 문구·번역본은 유지")
    void updateCheckedOnly() {
        Case caseEntity = caseOf();
        CaseChecklistItem item = itemOf(caseEntity, 31L, "Take photos", "사진 찍기", ChecklistSource.AI, 1);
        givenItem(caseEntity, item);

        ChecklistItemResponse response = caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(true, null));

        assertThat(item.isChecked()).isTrue();
        assertThat(item.getText()).isEqualTo("Take photos");
        assertThat(item.getTextNative()).isEqualTo("사진 찍기");
        assertThat(response.isChecked()).isTrue();
        assertThat(response.getItemId()).isEqualTo(31L);
    }

    @Test
    @DisplayName("수정: USER 항목 문구만 바꾸면 trim된 문구 저장, 체크 여부는 유지")
    void updateTextOnlyUserItem() {
        Case caseEntity = caseOf();
        CaseChecklistItem item = itemOf(caseEntity, 32L, "Old", null, ChecklistSource.USER, 2);
        givenItem(caseEntity, item);

        ChecklistItemResponse response = caseChecklistService.updateItem(6L, 32L, 1L, updateRequest(null, "  New text  "));

        assertThat(item.getText()).isEqualTo("New text");
        assertThat(item.isChecked()).isFalse();
        assertThat(item.getSource()).isEqualTo(ChecklistSource.USER);
        assertThat(response.getText().en()).isEqualTo("New text");
    }

    @Test
    @DisplayName("수정: AI 항목 문구 수정 시 번역본 비움, source는 AI 유지")
    void updateTextAiItem() {
        Case caseEntity = caseOf();
        CaseChecklistItem item = itemOf(caseEntity, 31L, "Take photos", "사진 찍기", ChecklistSource.AI, 1);
        givenItem(caseEntity, item);

        ChecklistItemResponse response = caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(null, "Take more photos"));

        assertThat(item.getText()).isEqualTo("Take more photos");
        assertThat(item.getTextNative()).isNull();
        assertThat(item.getSource()).isEqualTo(ChecklistSource.AI);
        assertThat(response.getText().nativeText()).isNull();
        assertThat(response.getSource()).isEqualTo(ChecklistSource.AI);
    }

    @Test
    @DisplayName("수정: 문구가 그대로면 번역본 유지 (trim 후 비교)")
    void updateSameText() {
        Case caseEntity = caseOf();
        CaseChecklistItem item = itemOf(caseEntity, 31L, "Take photos", "사진 찍기", ChecklistSource.AI, 1);
        givenItem(caseEntity, item);

        caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(true, " Take photos "));

        assertThat(item.getTextNative()).isEqualTo("사진 찍기");
        assertThat(item.isChecked()).isTrue();
    }

    @Test
    @DisplayName("수정: checked·text 둘 다 없으면 EMPTY_CHECKLIST_UPDATE (케이스 조회 전에 검증)")
    void updateEmpty() {
        assertThatThrownBy(() -> caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.EMPTY_CHECKLIST_UPDATE);
        verify(caseService, never()).findOwnedCase(any(), any());
    }

    @Test
    @DisplayName("수정: 공백만 있는 문구는 INVALID_CHECKLIST_TEXT이고 항목 조회·반영 없음")
    void updateBlankText() {
        assertThatThrownBy(() -> caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(true, "   ")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_CHECKLIST_TEXT);
        verify(caseService, never()).findOwnedCase(any(), any());
        verify(caseChecklistItemRepository, never()).findByIdAndCaseEntity(any(), any());
    }

    @Test
    @DisplayName("수정: 항목이 없거나 다른 케이스의 항목이면 CHECKLIST_ITEM_NOT_FOUND")
    void updateNotFound() {
        Case caseEntity = caseOf();
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseChecklistItemRepository.findByIdAndCaseEntity(99L, caseEntity)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseChecklistService.updateItem(6L, 99L, 1L, updateRequest(true, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CHECKLIST_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("수정: 케이스 조회 실패(404)는 그대로 전파되고 항목 조회는 하지 않음")
    void updateCaseFailure() {
        given(caseService.findOwnedCase(6L, 1L)).willThrow(new CustomException(CaseErrorCode.CASE_NOT_FOUND));

        assertThatThrownBy(() -> caseChecklistService.updateItem(6L, 31L, 1L, updateRequest(true, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CASE_NOT_FOUND);
        verify(caseChecklistItemRepository, never()).findByIdAndCaseEntity(any(), any());
    }

    // ========== 삭제 ==========

    @Test
    @DisplayName("삭제: AI 항목도 삭제")
    void deleteItem() {
        Case caseEntity = caseOf();
        CaseChecklistItem item = itemOf(caseEntity, 31L, "Take photos", "사진 찍기", ChecklistSource.AI, 1);
        givenItem(caseEntity, item);

        caseChecklistService.deleteItem(6L, 31L, 1L);

        verify(caseChecklistItemRepository).delete(item);
    }

    @Test
    @DisplayName("삭제: 항목이 없으면 CHECKLIST_ITEM_NOT_FOUND")
    void deleteNotFound() {
        Case caseEntity = caseOf();
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseChecklistItemRepository.findByIdAndCaseEntity(99L, caseEntity)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseChecklistService.deleteItem(6L, 99L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CHECKLIST_ITEM_NOT_FOUND);
        verify(caseChecklistItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제: 케이스 조회 실패(403)는 그대로 전파")
    void deleteCaseFailure() {
        given(caseService.findOwnedCase(6L, 1L)).willThrow(new CustomException(CaseErrorCode.NOT_CASE_OWNER));

        assertThatThrownBy(() -> caseChecklistService.deleteItem(6L, 31L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
        verify(caseChecklistItemRepository, never()).delete(any());
    }

    // ========== 테스트 픽스처 ==========

    private void givenItem(Case caseEntity, CaseChecklistItem item) {
        given(caseService.findOwnedCase(6L, 1L)).willReturn(caseEntity);
        given(caseChecklistItemRepository.findByIdAndCaseEntity(item.getId(), caseEntity)).willReturn(Optional.of(item));
    }

    private Case caseOf() {
        Case caseEntity = Case.builder()
                .caseNumber("C-006")
                .title("Water Leak in Bathroom")
                .stage(CaseStage.AWAITING_RESPONSE)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
        return caseEntity;
    }

    private CaseChecklistItem itemOf(Case caseEntity, Long id, String text, String textNative, ChecklistSource source, int sortOrder) {
        CaseChecklistItem item = CaseChecklistItem.builder()
                .caseEntity(caseEntity)
                .text(text)
                .textNative(textNative)
                .source(source)
                .sortOrder(sortOrder)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private ChecklistItemCreateRequest createRequest(String text) {
        ChecklistItemCreateRequest request = new ChecklistItemCreateRequest();
        ReflectionTestUtils.setField(request, "text", text);
        return request;
    }

    private ChecklistItemUpdateRequest updateRequest(Boolean checked, String text) {
        ChecklistItemUpdateRequest request = new ChecklistItemUpdateRequest();
        ReflectionTestUtils.setField(request, "checked", checked);
        ReflectionTestUtils.setField(request, "text", text);
        return request;
    }
}
