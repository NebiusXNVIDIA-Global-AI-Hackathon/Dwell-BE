package com.dwell.domain.building.service;

import com.dwell.domain.building.dto.request.InspectionCreateRequest;
import com.dwell.domain.building.dto.request.PlaceUpdateRequest;
import com.dwell.domain.building.dto.response.AttentionResponse;
import com.dwell.domain.building.dto.response.BuildingSignalsResponse;
import com.dwell.domain.building.dto.response.HpdHistoryResponse;
import com.dwell.domain.building.dto.response.LandlordInsightResponse;
import com.dwell.domain.building.dto.response.PlaceSummaryResponse;
import com.dwell.domain.building.dto.response.RecordsInsightResponse;
import com.dwell.domain.building.dto.response.SignalDetailResponse;
import com.dwell.domain.building.dto.response.SignalItemResponse;
import com.dwell.domain.building.dto.response.ViolationClassCountResponse;
import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.BuildingSignal;
import com.dwell.domain.building.entity.HpdViolation;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.enums.FollowUpReason;
import com.dwell.domain.building.enums.HpdHistoryTab;
import com.dwell.domain.building.enums.HpdRecordType;
import com.dwell.domain.building.enums.HpdStatus;
import com.dwell.domain.building.enums.InsightStatus;
import com.dwell.domain.building.enums.LandlordRating;
import com.dwell.domain.building.enums.UsState;
import com.dwell.domain.building.exception.BuildingErrorCode;
import com.dwell.domain.building.repository.BuildingSignalRepository;
import com.dwell.domain.building.repository.HpdViolationRepository;
import com.dwell.domain.building.repository.InspectionResultRepository;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.community.service.CommunityService;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaceServiceImplTest {

    @Mock
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private BuildingSignalRepository buildingSignalRepository;
    @Mock
    private HpdViolationRepository hpdViolationRepository;
    @Mock
    private InspectionResultRepository inspectionResultRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CommunityService communityService;
    @Mock
    private BuildingService buildingService;

    private PlaceServiceImpl placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceServiceImpl(userPlaceRepository, buildingSignalRepository, hpdViolationRepository,
                inspectionResultRepository, caseRepository, communityService, buildingService);
    }

    @Test
    @DisplayName("집 등록이 없으면 PLACE_NOT_REGISTERED")
    void placeNotRegistered() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.getSignals(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BuildingErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(buildingSignalRepository);
    }

    @Test
    @DisplayName("시그널: 내 진행 중 케이스와 같은 이슈 중 첫 번째(신고 많은 순)가 primary, 나머지는 Rising Issue")
    void getSignals() {
        placeIn(10L);
        BuildingSignal heat = signal(1L, IssueCategory.HEAT_HOT_WATER, 6);
        BuildingSignal leak = signal(2L, IssueCategory.WATER_FLOODING, 5);
        given(leak.getWindowDays()).willReturn(30);
        given(leak.getBreakdown()).willReturn(Map.of("by_location", List.of("Bathroom")));
        BuildingSignal pests = signal(3L, IssueCategory.PESTS, 2);
        given(buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(10L))
                .willReturn(List.of(heat, leak, pests));
        given(caseRepository.findActiveIssueCategoriesByUserId(1L)).willReturn(List.of(IssueCategory.WATER_FLOODING));

        BuildingSignalsResponse response = placeService.getSignals(1L);

        assertThat(response.getPrimary().getSignalId()).isEqualTo(2L);
        assertThat(response.getPrimary().getTitle()).isEqualTo("Water leak pattern detected");
        assertThat(response.getPrimary().getWindowLabel()).isEqualTo("30 days");
        assertThat(response.getPrimary().getReportedIn()).containsExactly("Bathroom");
        assertThat(response.getRisingIssues()).extracting(SignalItemResponse::getSignalId).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("시그널: 진행 중 케이스가 없으면 primary는 null, 전부 Rising Issue")
    void getSignalsWithoutCase() {
        placeIn(10L);
        BuildingSignal heat = signal(1L, IssueCategory.HEAT_HOT_WATER, 6);
        given(buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(10L)).willReturn(List.of(heat));
        given(caseRepository.findActiveIssueCategoriesByUserId(1L)).willReturn(List.of());

        BuildingSignalsResponse response = placeService.getSignals(1L);

        assertThat(response.getPrimary()).isNull();
        assertThat(response.getRisingIssues()).singleElement().extracting(SignalItemResponse::getTitle).isEqualTo("Heating issues");
    }

    @Test
    @DisplayName("Issue Details: 다른 건물 시그널이면 SIGNAL_NOT_FOUND")
    void getSignalOtherBuilding() {
        placeIn(10L);
        BuildingSignal other = mock(BuildingSignal.class);
        Building otherBuilding = building(20L);
        given(other.getBuilding()).willReturn(otherBuilding);
        given(buildingSignalRepository.findById(5L)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> placeService.getSignal(1L, 5L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BuildingErrorCode.SIGNAL_NOT_FOUND);
    }

    @Test
    @DisplayName("Issue Details: insight와 이슈 관련 커뮤니티 글을 함께 줌")
    void getSignal() {
        Building building = placeIn(10L).getBuilding();
        BuildingSignal heat = mock(BuildingSignal.class);
        given(heat.getId()).willReturn(1L);
        given(heat.getIssueCategory()).willReturn(IssueCategory.HEAT_HOT_WATER);
        given(heat.getBuilding()).willReturn(building);
        given(buildingSignalRepository.findById(1L)).willReturn(Optional.of(heat));
        given(caseRepository.findActiveIssueCategoriesByUserId(1L)).willReturn(List.of(IssueCategory.HEAT_HOT_WATER));
        given(communityService.getIssuePosts(10L, IssueCategory.HEAT_HOT_WATER)).willReturn(List.of());

        SignalDetailResponse response = placeService.getSignal(1L, 1L);

        assertThat(response.getTitle()).isEqualTo("Heating issues");
        assertThat(response.isRelatedToMyCase()).isTrue();
        assertThat(response.getPosts()).isEmpty();
    }

    @Test
    @DisplayName("Check My Unit: 체크리스트에 없는 증상이면 INVALID_SYMPTOM, 저장하지 않음")
    void createInspectionInvalidSymptom() {
        Building building = placeIn(10L).getBuilding();
        BuildingSignal pests = mock(BuildingSignal.class);
        given(pests.getBuilding()).willReturn(building);
        given(pests.getGuidedInspection()).willReturn(List.of(Map.of("key", "ROACHES_SEEN", "label", "Roaches seen")));
        given(buildingSignalRepository.findById(3L)).willReturn(Optional.of(pests));
        InspectionCreateRequest request = new InspectionCreateRequest();
        request.setSignalId(3L);
        request.setSymptoms(List.of("ROACHES_SEEN", "NOPE"));

        assertThatThrownBy(() -> placeService.createInspection(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BuildingErrorCode.INVALID_SYMPTOM);
        verifyNoInteractions(inspectionResultRepository);
    }

    @Test
    @DisplayName("Landlord: closed 위반 평균 수리일이 NYC 평균(30일)의 1.2배를 넘으면 SLOW_TO_FIX, timer 48h")
    void landlordSlowToFix() {
        placeIn(10L);
        LocalDate t = LocalDate.now();
        given(hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(any(), any())).willReturn(List.of(
                hpd(HpdRecordType.VIOLATION, HpdStatus.CLOSED, "B", t.minusDays(100), t.minusDays(60)),
                hpd(HpdRecordType.VIOLATION, HpdStatus.CLOSED, "A", t.minusDays(90), t.minusDays(50)),
                hpd(HpdRecordType.VIOLATION, HpdStatus.CLOSED, "B", t.minusDays(80), t.minusDays(40)),
                hpd(HpdRecordType.VIOLATION, HpdStatus.OPEN, "C", t.minusDays(10), null),
                hpd(HpdRecordType.COMPLAINT, HpdStatus.OPEN, null, t.minusDays(5), null)));

        LandlordInsightResponse response = placeService.getLandlordInsight(1L);

        assertThat(response.getTrackRecord().getAvgDaysToFix()).isEqualTo(40);
        assertThat(response.getTrackRecord().getRating()).isEqualTo(LandlordRating.SLOW_TO_FIX);
        assertThat(response.getTrackRecord().getViolations().getTotal()).isEqualTo(4);
        assertThat(response.getTrackRecord().getClassC().getOpen()).isEqualTo(1);
        assertThat(response.getFollowUpTimer().getAppliedHours()).isEqualTo(48);
        assertThat(response.getFollowUpTimer().getReason()).isEqualTo(FollowUpReason.SHORTENED);
    }

    @Test
    @DisplayName("Landlord: closed 위반이 3건 미만이면 NOT_ENOUGH_DATA, 점수 없음, timer 72h")
    void landlordNotEnoughData() {
        placeIn(10L);
        LocalDate t = LocalDate.now();
        given(hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(any(), any())).willReturn(List.of(
                hpd(HpdRecordType.VIOLATION, HpdStatus.CLOSED, "B", t.minusDays(100), t.minusDays(90)),
                hpd(HpdRecordType.VIOLATION, HpdStatus.OPEN, "C", t.minusDays(10), null)));

        LandlordInsightResponse response = placeService.getLandlordInsight(1L);

        assertThat(response.getTrackRecord().getRating()).isEqualTo(LandlordRating.NOT_ENOUGH_DATA);
        assertThat(response.getTrackRecord().getAvgDaysToFix()).isNull();
        assertThat(response.getTrackRecord().getSource()).isEqualTo("NYC HPD · 1 closed violation (3+ needed)");
        assertThat(response.getFollowUpTimer().getAppliedHours()).isEqualTo(72);
        assertThat(response.getFollowUpTimer().getReason()).isEqualTo(FollowUpReason.NOT_ENOUGH_HISTORY);
    }

    @Test
    @DisplayName("Records: 최근 1년 같은 민원 3회 이상이면 반복 민원, 기록 3건 미만이면 추세 없음")
    void recordsInsight() {
        placeIn(10L);
        LocalDate t = LocalDate.now();
        given(hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(any(), any())).willReturn(List.of(
                complaint("Water leak", t.minusDays(10)),
                complaint("Water leak", t.minusDays(40)),
                complaint("Water leak", t.minusDays(70)),
                complaint("Pests", t.minusDays(500))));

        RecordsInsightResponse response = placeService.getRecordsInsight(1L);

        assertThat(response.getStatus()).isEqualTo(InsightStatus.OK);
        assertThat(response.getRepeatedIssue().getCategory()).isEqualTo("Water leak");
        assertThat(response.getRepeatedIssue().getCount()).isEqualTo(3);
        assertThat(response.getComplaintsPerMonth()).hasSize(36);

        given(hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(any(), any()))
                .willReturn(List.of(complaint("Pests", t.minusDays(5))));
        RecordsInsightResponse few = placeService.getRecordsInsight(1L);
        assertThat(few.getStatus()).isEqualTo(InsightStatus.NOT_ENOUGH_DATA);
        assertThat(few.getComplaintsByCategory()).isNull();
        assertThat(few.getComplaintsPerMonth()).isNull();
    }

    @Test
    @DisplayName("HPD History Overview: 클래스 A/B/C는 항상 3개, 12분기 차트")
    void hpdHistoryOverview() {
        placeIn(10L);
        LocalDate t = LocalDate.now();
        given(hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(any(), any())).willReturn(List.of(
                hpd(HpdRecordType.VIOLATION, HpdStatus.OPEN, "C", t.minusDays(10), null),
                hpd(HpdRecordType.VIOLATION, HpdStatus.CLOSED, "C", t.minusDays(20), t.minusDays(5))));

        HpdHistoryResponse response = placeService.getHpdHistory(1L, HpdHistoryTab.OVERVIEW, 0, 20);

        assertThat(response.getViolationsByClass()).extracting(ViolationClassCountResponse::getViolationClass).containsExactly("A", "B", "C");
        assertThat(response.getViolationsByClass().get(2).getTotal()).isEqualTo(2);
        assertThat(response.getViolationsByClass().get(2).getOpen()).isEqualTo(1);
        assertThat(response.getPerQuarter()).hasSize(12);
        assertThat(response.getContent()).isNull();
    }

    @Test
    @DisplayName("집 주소 변경: 새 건물로 교체, 층·집주인 정보 초기화, 바뀐 주소로 요약 반환")
    void updatePlace() {
        Building oldBuilding = realBuilding(10L, "100 Old St");
        UserPlace place = UserPlace.builder()
                .building(oldBuilding)
                .unit("Apt 1A")
                .floor(1)
                .unitLine("A")
                .leaseInOwnName(true)
                .landlordName("Old Landlord")
                .build();
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Building newBuilding = realBuilding(20L, "254 W 107th St");
        given(buildingService.findOrCreate("1018790031", "254 W 107th St", "New York", UsState.NY, "10025")).willReturn(newBuilding);
        given(buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(20L)).willReturn(List.of());
        lenient().when(caseRepository.findActiveIssueCategoriesByUserId(1L)).thenReturn(List.of());

        PlaceSummaryResponse response = placeService.updatePlace(1L, updateRequest("Apt 4B"));

        assertThat(place.getBuilding()).isSameAs(newBuilding);
        assertThat(place.getUnit()).isEqualTo("Apt 4B");
        assertThat(place.getFloor()).isNull();
        assertThat(place.getUnitLine()).isNull();
        assertThat(place.getLandlordName()).isNull();
        assertThat(response.getAddress()).isEqualTo("254 W 107th St, Apt 4B, New York, NY 10025");
        assertThat(response.getBbl()).isEqualTo("1018790031");
    }

    @Test
    @DisplayName("집 주소 변경: 등록된 집이 없으면 PLACE_NOT_REGISTERED, 건물 생성 X")
    void updatePlaceNotRegistered() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.updatePlace(1L, updateRequest("Apt 4B")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BuildingErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(buildingService);
    }

    @Test
    @DisplayName("Needs Your Attention: 신고가 가장 많은 시그널 하나로 문구와 signalId")
    void getAttention() {
        BuildingSignal pests = mock(BuildingSignal.class);
        given(pests.getId()).willReturn(13L);
        given(pests.getIssueCategory()).willReturn(IssueCategory.PESTS);
        BuildingSignal heat = mock(BuildingSignal.class);
        given(buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(10L))
                .willReturn(List.of(pests, heat));

        AttentionResponse attention = placeService.getAttention(10L);

        assertThat(attention.getSignalId()).isEqualTo(13L);
        assertThat(attention.getText().en()).isEqualTo("Pest issues have been reported a lot in your building lately.");
        assertThat(attention.getText().nativeText()).isNull();
    }

    @Test
    @DisplayName("Needs Your Attention: 시그널이 없으면 null (카드 숨김)")
    void getAttentionWithoutSignal() {
        given(buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(10L)).willReturn(List.of());

        assertThat(placeService.getAttention(10L)).isNull();
    }

    private Building realBuilding(Long id, String street) {
        Building building = Building.builder()
                .bbl("1018790031")
                .street(street)
                .city("New York")
                .state(UsState.NY)
                .zip("10025")
                .build();
        ReflectionTestUtils.setField(building, "id", id);
        return building;
    }

    private PlaceUpdateRequest updateRequest(String unit) {
        PlaceUpdateRequest request = new PlaceUpdateRequest();
        ReflectionTestUtils.setField(request, "street", "254 W 107th St");
        ReflectionTestUtils.setField(request, "unit", unit);
        ReflectionTestUtils.setField(request, "city", "New York");
        ReflectionTestUtils.setField(request, "state", UsState.NY);
        ReflectionTestUtils.setField(request, "zip", "10025");
        ReflectionTestUtils.setField(request, "bbl", "1018790031");
        return request;
    }

    private UserPlace placeIn(Long buildingId) {
        UserPlace place = mock(UserPlace.class);
        Building building = building(buildingId);
        given(place.getBuilding()).willReturn(building);
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        return place;
    }

    private Building building(Long id) {
        Building building = mock(Building.class);
        given(building.getId()).willReturn(id);
        return building;
    }

    private BuildingSignal signal(Long id, IssueCategory category, int reportCount) {
        BuildingSignal signal = mock(BuildingSignal.class);
        given(signal.getId()).willReturn(id);
        given(signal.getIssueCategory()).willReturn(category);
        given(signal.getReportCount()).willReturn(reportCount);
        return signal;
    }

    private HpdViolation hpd(HpdRecordType type, HpdStatus status, String violationClass, LocalDate reportedAt, LocalDate closedAt) {
        return HpdViolation.builder()
                .recordType(type)
                .hpdViolationId("H1")
                .issueType(IssueType.OTHER)
                .category("Failure to maintain")
                .status(status)
                .violationClass(violationClass)
                .reportedAt(reportedAt)
                .closedAt(closedAt)
                .build();
    }

    private HpdViolation complaint(String category, LocalDate reportedAt) {
        return HpdViolation.builder()
                .recordType(HpdRecordType.COMPLAINT)
                .hpdViolationId("H1")
                .issueType(IssueType.OTHER)
                .category(category)
                .status(HpdStatus.OPEN)
                .reportedAt(reportedAt)
                .build();
    }
}
