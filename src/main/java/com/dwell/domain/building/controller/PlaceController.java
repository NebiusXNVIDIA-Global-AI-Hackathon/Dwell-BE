package com.dwell.domain.building.controller;

import com.dwell.domain.building.dto.request.InspectionCreateRequest;
import com.dwell.domain.building.dto.request.PlaceUpdateRequest;
import com.dwell.domain.building.dto.response.BuildingSignalsResponse;
import com.dwell.domain.building.dto.response.HpdHistoryResponse;
import com.dwell.domain.building.dto.response.InspectionCreateResponse;
import com.dwell.domain.building.dto.response.InspectionGuideResponse;
import com.dwell.domain.building.dto.response.LandlordInsightResponse;
import com.dwell.domain.building.dto.response.PlaceSummaryResponse;
import com.dwell.domain.building.dto.response.RecordsInsightResponse;
import com.dwell.domain.building.dto.response.SignalDetailResponse;
import com.dwell.domain.building.enums.HpdHistoryTab;
import com.dwell.domain.building.service.PlaceService;
import com.dwell.domain.community.dto.request.LandlordPostSearchRequest;
import com.dwell.domain.community.dto.response.LandlordPostListResponse;
import com.dwell.domain.community.dto.response.LandlordPostSummaryResponse;
import com.dwell.domain.community.service.CommunityService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Place", description = "내 건물 (Building Signals · Check My Unit · Building Insights · HPD) API")
@RestController
@RequestMapping("/api/v1/places/me")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final CommunityService communityService;

    // 내 건물 요약 컨트롤러
    @Operation(summary = "내 건물 요약",
            description = "My Place 메인: 주소와 Rising Issue 카드. risingIssues가 []이면 All clear!!! 카드를 보여주세요.")
    @GetMapping
    public ResponseEntity<BaseResponse<PlaceSummaryResponse>> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 내 건물 요약 서비스 호출
        PlaceSummaryResponse response = placeService.getSummary(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Place summary retrieved.", response));
    }

    // 집 주소 변경 컨트롤러
    @Operation(summary = "집 주소 변경",
            description = "My Home 화면에서 집 주소를 바꾸고, 바뀐 집 기준 내 건물 요약을 줍니다. 기존 케이스는 이전 주소에 남습니다.")
    @PutMapping
    public ResponseEntity<BaseResponse<PlaceSummaryResponse>> updatePlace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlaceUpdateRequest request) {

        // 집 주소 변경 서비스 호출
        PlaceSummaryResponse response = placeService.updatePlace(userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Home address updated.", response));
    }

    // Building Signals 목록 컨트롤러
    @Operation(summary = "Building Signals 목록",
            description = "내 진행 중 케이스와 같은 이슈의 가장 강한 패턴(primary, 없으면 null)과 나머지 Rising Issue를 줍니다.")
    @GetMapping("/building-signals")
    public ResponseEntity<BaseResponse<BuildingSignalsResponse>> getSignals(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Building Signals 서비스 호출
        BuildingSignalsResponse response = placeService.getSignals(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Building signals retrieved.", response));
    }

    // Issue Details 컨트롤러
    @Operation(summary = "Issue Details (시그널 상세)",
            description = "내 건물 시그널의 Deaver's insight를 줍니다. 다른 건물 시그널은 404입니다.")
    @GetMapping("/building-signals/{signalId}")
    public ResponseEntity<BaseResponse<SignalDetailResponse>> getSignal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long signalId) {

        // Issue Details 서비스 호출
        SignalDetailResponse response = placeService.getSignal(userDetails.getUser().getId(), signalId);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Building signal retrieved.", response));
    }

    // Check My Unit 체크리스트 컨트롤러
    @Operation(summary = "Check My Unit 체크리스트",
            description = "시그널의 증상 체크리스트를 줍니다. 체크한 key를 POST /places/me/inspections로 보내세요.")
    @GetMapping("/inspection")
    public ResponseEntity<BaseResponse<InspectionGuideResponse>> getInspectionGuide(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long signalId) {

        // 체크리스트 서비스 호출
        InspectionGuideResponse response = placeService.getInspectionGuide(userDetails.getUser().getId(), signalId);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Inspection checklist retrieved.", response));
    }

    // Check My Unit 결과 저장 컨트롤러
    @Operation(summary = "Check My Unit 결과 저장",
            description = "체크한 증상(1개 이상)을 저장하고 Create Case 프리필을 줍니다. 체크리스트에 없는 증상이면 400입니다.")
    @PostMapping("/inspections")
    public ResponseEntity<BaseResponse<InspectionCreateResponse>> createInspection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InspectionCreateRequest request) {

        // 결과 저장 서비스 호출
        InspectionCreateResponse response = placeService.createInspection(userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Inspection saved.", response));
    }

    // Building Insights · Landlord 탭 컨트롤러
    @Operation(summary = "Building Insights · Landlord 탭",
            description = "최근 3년 HPD 위반 평균 수리일로 집주인 배지와 Follow-up timer를 줍니다. closed 위반 3건 미만이면 NOT_ENOUGH_DATA입니다.")
    @GetMapping("/insights/landlord")
    public ResponseEntity<BaseResponse<LandlordInsightResponse>> getLandlordInsight(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Landlord 탭 서비스 호출
        LandlordInsightResponse response = placeService.getLandlordInsight(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Landlord insight retrieved.", response));
    }

    // Building Insights · Tenants 탭 컨트롤러
    @Operation(summary = "Building Insights · Tenants 탭",
            description = "내 건물 게시글 중 집주인을 언급한 글 수·분류별 수·미리보기 3개를 줍니다. 3개 미만이면 NOT_ENOUGH_DATA입니다. "
                    + "집주인 언급·분류는 아직 제목·내용 키워드로 판단합니다.")
    @GetMapping("/insights/tenants")
    public ResponseEntity<BaseResponse<LandlordPostSummaryResponse>> getTenantsInsight(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 집주인 언급 글 요약 서비스 호출
        LandlordPostSummaryResponse response = communityService.getLandlordPostSummary(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Tenant posts summary retrieved.", response));
    }

    // Posts about landlord 컨트롤러
    @Operation(summary = "Posts about landlord",
            description = "내 건물 집주인 언급 글을 검색(q)·분류(topic)·이슈(issueType)·층(floorBand)·기간(posted)으로 거르고 정렬(sort)해서 줍니다. "
                    + "topic·issueType·floorBand는 여러 번 보낼 수 있습니다 (예: topic=SLOW_REPAIRS&topic=NO_REPLY).")
    @GetMapping("/insights/landlord-posts")
    public ResponseEntity<BaseResponse<LandlordPostListResponse>> getLandlordPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @ModelAttribute LandlordPostSearchRequest request) {

        // 집주인 언급 글 목록 서비스 호출
        LandlordPostListResponse response = communityService.getLandlordPosts(userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Landlord posts retrieved.", response));
    }

    // Building Insights · Records 탭 컨트롤러
    @Operation(summary = "Building Insights · Records 탭",
            description = "최근 3년 HPD 민원·위반 요약, 카테고리별·반복 민원, 월별 추세를 줍니다. 기록 3건 미만이면 NOT_ENOUGH_DATA입니다.")
    @GetMapping("/insights/records")
    public ResponseEntity<BaseResponse<RecordsInsightResponse>> getRecordsInsight(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Records 탭 서비스 호출
        RecordsInsightResponse response = placeService.getRecordsInsight(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Building records retrieved.", response));
    }

    // HPD History 컨트롤러
    @Operation(summary = "HPD History",
            description = "tab=OVERVIEW(기본)면 클래스별 위반·분기별 차트, COMPLAINTS / VIOLATIONS면 최근 3년 기록 목록(최신 순, size 최대 50)을 줍니다.")
    @GetMapping("/hpd-history")
    public ResponseEntity<BaseResponse<HpdHistoryResponse>> getHpdHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "OVERVIEW") HpdHistoryTab tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // HPD History 서비스 호출
        HpdHistoryResponse response = placeService.getHpdHistory(userDetails.getUser().getId(), tab, page, size);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "HPD history retrieved.", response));
    }
}
