package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.dto.response.CaseTimelineResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.CaseStatus;
import com.dwell.domain.cases.enums.StatusBadge;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseChecklistItemRepository caseChecklistItemRepository;
    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private FileRepository fileRepository;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(caseRepository, caseChecklistItemRepository, caseEventRepository, noticeRepository,
                evidenceRepository, fileRepository, new CaseBannerFactory(), new CaseTimelineStepFactory(), new CaseTimelineEventFactory());
    }

    @Test
    @DisplayName("없는 케이스면 CASE_NOT_FOUND")
    void caseNotFound() {
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.getCaseOverview(6L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CASE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사람 케이스면 NOT_CASE_OWNER")
    void notOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.getCaseOverview(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
    }

    @Test
    @DisplayName("상세 조회: stage에서 status·배지 파생, 주소 조합, 빈 증거·체크리스트")
    void overview() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());
        given(caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(any())).willReturn(List.of());
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(any())).willReturn(List.of());

        CaseOverviewResponse response = caseService.getCaseOverview(6L, 1L);

        assertThat(response.getCaseNumber()).isEqualTo("C-006");
        assertThat(response.getAddress()).isEqualTo("123 Main St, Apt 4B");
        assertThat(response.getStatus()).isEqualTo(CaseStatus.WAITING);
        assertThat(response.getStatusBadge()).isEqualTo(StatusBadge.IN_PROGRESS);
        assertThat(response.getCurrentStatus().getTitle().en()).isEqualTo("Waiting for your landlord's response");
        assertThat(response.getTimelineSteps()).isNotEmpty();
        assertThat(response.getEvidence().getCount()).isZero();
        assertThat(response.getChecklist().getItems()).isEmpty();
    }

    @Test
    @DisplayName("타임라인 조회: 헤더(번호·주소·배지) + 이벤트 카드")
    void timeline() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of(
                CaseBannerFactoryTest.event(caseEntity, CaseEventType.CASE_CREATED, LocalDateTime.of(2026, 9, 28, 20, 12), null)));
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());

        CaseTimelineResponse response = caseService.getCaseTimeline(6L, 1L);

        assertThat(response.getHeader().getCaseNumber()).isEqualTo("C-006");
        assertThat(response.getHeader().getAddress()).isEqualTo("123 Main St, Apt 4B");
        assertThat(response.getHeader().getStatusBadge()).isEqualTo(StatusBadge.IN_PROGRESS);
        assertThat(response.getEvents()).extracting(event -> event.getTitle().en()).containsExactly("Case Created");
    }

    @Test
    @DisplayName("타임라인 조회: 다른 사람 케이스면 NOT_CASE_OWNER")
    void timelineNotOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.getCaseTimeline(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
    }

    // ========== 테스트 픽스처 ==========

    private User owner(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private Case caseOf(User user, CaseStage stage) {
        Building building = mock(Building.class);
        lenient().when(building.getStreet()).thenReturn("123 Main St");

        Case caseEntity = Case.builder()
                .caseNumber("C-006")
                .user(user)
                .building(building)
                .unit("Apt 4B")
                .title("Water Leak in Bathroom · Ceiling")
                .stage(stage)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
        return caseEntity;
    }
}
