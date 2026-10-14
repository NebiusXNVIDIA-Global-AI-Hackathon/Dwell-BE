package com.dwell.domain.home.service;

import com.dwell.domain.building.dto.response.AttentionResponse;
import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.building.service.PlaceService;
import com.dwell.domain.cases.dto.response.CurrentStatusResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.IssueDetail;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.cases.service.CaseBannerFactory;
import com.dwell.domain.cases.service.CaseTimelineStepFactory;
import com.dwell.domain.home.dto.response.HomeCaseResponse;
import com.dwell.domain.home.dto.response.HomeResponse;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.enums.Role;
import com.dwell.domain.user.exception.UserErrorCode;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.Location;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private CaseChecklistItemRepository caseChecklistItemRepository;
    @Mock
    private CaseTimelineStepFactory caseTimelineStepFactory;
    @Mock
    private CaseBannerFactory caseBannerFactory;
    @Mock
    private PlaceService placeService;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(userRepository, userPlaceRepository, caseRepository, caseEventRepository,
                noticeRepository, caseChecklistItemRepository, caseTimelineStepFactory, caseBannerFactory, placeService);
    }

    @Test
    @DisplayName("홈: 카드 제목·위치, 응답 기한 있는 케이스가 먼저, 닉네임")
    void getHome() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userOf()));

        // 기한 없음, 최근 업데이트 (Water Leak, Bathroom - Ceiling)
        Case leak = caseOf(6L, IssueDetail.WATER_LEAK.name(), null, Location.BATHROOM, AffectedArea.CEILING,
                LocalDateTime.now());
        // 기한 있음, 오래된 업데이트 (Other 입력값, Entire Unit은 공간만)
        Case other = caseOf(5L, IssueDetail.OTHER, "Rainwater coming in", Location.ENTIRE_UNIT, null,
                LocalDateTime.now().minusDays(3));
        given(caseRepository.findAllByUserIdAndDeletedAtIsNullAndStageNot(1L, CaseStage.RESOLVED)).willReturn(List.of(leak, other));
        stubCaseDetails();
        given(caseBannerFactory.create(eq(leak), any(), any(), any())).willReturn(statusDueAt(null));
        given(caseBannerFactory.create(eq(other), any(), any(), any())).willReturn(statusDueAt(LocalDateTime.now().plusDays(1)));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        HomeResponse response = homeService.getHome(1L);

        assertThat(response.getUser().getNickname()).isEqualTo("seohu");
        assertThat(response.getCases()).extracting(HomeCaseResponse::getCaseId).containsExactly(5L, 6L);
        HomeCaseResponse otherCard = response.getCases().get(0);
        assertThat(otherCard.getTitle()).isEqualTo("Rainwater coming in");
        assertThat(otherCard.getLocationLabel()).isEqualTo(Location.ENTIRE_UNIT.getLabel());
        HomeCaseResponse leakCard = response.getCases().get(1);
        assertThat(leakCard.getTitle()).isEqualTo(IssueDetail.WATER_LEAK.getLabel());
        assertThat(leakCard.getLocationLabel()).isEqualTo(Location.BATHROOM.getLabel() + " - " + AffectedArea.CEILING.getLabel());
    }

    @Test
    @DisplayName("홈: 응답 기한이 없으면 최근 업데이트 순, 케이스가 없으면 []")
    void getHomeSortedByUpdate() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userOf()));
        Case older = caseOf(5L, IssueDetail.WATER_LEAK.name(), null, Location.BATHROOM, AffectedArea.CEILING,
                LocalDateTime.now().minusDays(3));
        Case newer = caseOf(6L, IssueDetail.WATER_LEAK.name(), null, Location.KITCHEN, AffectedArea.CEILING,
                LocalDateTime.now());
        given(caseRepository.findAllByUserIdAndDeletedAtIsNullAndStageNot(1L, CaseStage.RESOLVED)).willReturn(List.of(older, newer));
        stubCaseDetails();
        given(caseBannerFactory.create(any(), any(), any(), any())).willReturn(statusDueAt(null));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        HomeResponse response = homeService.getHome(1L);

        assertThat(response.getCases()).extracting(HomeCaseResponse::getCaseId).containsExactly(6L, 5L);
    }

    @Test
    @DisplayName("홈: 집이 있으면 내 건물 attention, 없으면 null (PlaceService 호출 X)")
    void getHomeAttention() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userOf()));
        given(caseRepository.findAllByUserIdAndDeletedAtIsNullAndStageNot(1L, CaseStage.RESOLVED)).willReturn(List.of());
        UserPlace place = mock(UserPlace.class);
        Building building = mock(Building.class);
        given(building.getId()).willReturn(10L);
        given(place.getBuilding()).willReturn(building);
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        AttentionResponse attention = AttentionResponse.builder()
                .text(new LocalizedText("Pest issues have been reported a lot in your building lately.", null))
                .signalId(13L)
                .build();
        given(placeService.getAttention(10L)).willReturn(attention);

        HomeResponse response = homeService.getHome(1L);

        assertThat(response.getCases()).isEmpty();
        assertThat(response.getAttention()).isSameAs(attention);
    }

    @Test
    @DisplayName("홈: 집 등록이 없으면 attention null, PlaceService 호출 X")
    void getHomeWithoutPlace() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userOf()));
        given(caseRepository.findAllByUserIdAndDeletedAtIsNullAndStageNot(1L, CaseStage.RESOLVED)).willReturn(List.of());
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        HomeResponse response = homeService.getHome(1L);

        assertThat(response.getAttention()).isNull();
        verifyNoInteractions(placeService);
    }

    @Test
    @DisplayName("홈: 없는 사용자면 USER_NOT_FOUND")
    void getHomeUserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> homeService.getHome(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ========== 픽스처 ==========

    private void stubCaseDetails() {
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());
        given(caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(any())).willReturn(List.of());
        given(caseTimelineStepFactory.create(any(), any())).willReturn(List.of());
    }

    private User userOf() {
        User user = User.builder()
                .email("test01@gmail.com")
                .nickname("seohu")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Case caseOf(Long id, String issueDetail, String issueDetailOther, Location location,
                        AffectedArea affectedArea, LocalDateTime modifiedAt) {
        Case caseEntity = mock(Case.class, withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        given(caseEntity.getId()).willReturn(id);
        given(caseEntity.getCaseNumber()).willReturn("C-00" + id);
        given(caseEntity.getIssueDetail()).willReturn(issueDetail);
        given(caseEntity.getIssueDetailOther()).willReturn(issueDetailOther);
        given(caseEntity.getLocation()).willReturn(location);
        given(caseEntity.getAffectedArea()).willReturn(affectedArea);
        given(caseEntity.getStage()).willReturn(CaseStage.AWAITING_RESPONSE);
        given(caseEntity.getModifiedAt()).willReturn(modifiedAt);
        return caseEntity;
    }

    private CurrentStatusResponse statusDueAt(LocalDateTime dueAt) {
        return CurrentStatusResponse.builder()
                .dueAt(dueAt)
                .build();
    }
}
