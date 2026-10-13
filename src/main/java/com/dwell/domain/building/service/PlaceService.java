package com.dwell.domain.building.service;

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

public interface PlaceService {

    /**
     * [내 건물 요약 서비스]
     * @author 김민호
     * @description: My Place 메인. 주소와 Rising Issue 카드(Building Signals의 Rising Issue와 같은 목록)를 줍니다.
     *
     * @request userId
     * @return PlaceSummaryResponse(address, bbl, risingIssues)
     */
    PlaceSummaryResponse getSummary(Long userId);

    /**
     * [집 주소 변경 서비스]
     * @author 김정모
     * @description: My Home 화면에서 집 주소를 바꿉니다. BBL로 건물을 찾거나 만들고 UserPlace만 교체합니다.
     *               기존 케이스는 케이스에 저장된 건물·호수 그대로라 이전 주소에 남습니다.
     *
     * @request userId, PlaceUpdateRequest(street, unit, city, state, zip, bbl)
     * @return PlaceSummaryResponse(address, bbl, risingIssues)
     */
    PlaceSummaryResponse updatePlace(Long userId, PlaceUpdateRequest request);

    /**
     * [Building Signals 목록 서비스]
     * @author 김민호
     * @description: 내 진행 중 케이스와 같은 이슈 중 신고가 가장 많은 시그널을 메인 패턴(primary)으로,
     *               나머지를 Rising Issue로 줍니다 (신고 많은 순).
     *
     * @request userId
     * @return BuildingSignalsResponse(primary, risingIssues)
     */
    BuildingSignalsResponse getSignals(Long userId);

    /**
     * [Issue Details 서비스]
     * @author 김민호
     * @description: 내 건물 시그널의 상세(Deaver's insight + 이슈 관련 이웃 게시글)를 줍니다. 다른 건물 시그널은 없는 시그널로 처리합니다.
     *
     * @request userId, signalId
     * @return SignalDetailResponse(signalId, title, issueCategory, relatedToMyCase, insight, posts)
     */
    SignalDetailResponse getSignal(Long userId, Long signalId);

    /**
     * [Check My Unit 체크리스트 서비스]
     * @author 김민호
     * @description: 시그널의 증상 체크리스트(guided inspection)를 줍니다.
     *
     * @request userId, signalId
     * @return InspectionGuideResponse(signalId, issueCategory, subtitle, symptoms)
     */
    InspectionGuideResponse getInspectionGuide(Long userId, Long signalId);

    /**
     * [Check My Unit 결과 저장 서비스]
     * @author 김민호
     * @description: 체크한 증상을 저장하고(FOUND_ISSUE), Create Case 프리필을 줍니다.
     *               체크리스트에 없는 증상이면 400입니다.
     *
     * @request userId, InspectionCreateRequest(signalId, symptoms)
     * @return InspectionCreateResponse(inspectionId, matchedSigns, prefill)
     */
    InspectionCreateResponse createInspection(Long userId, InspectionCreateRequest request);

    /**
     * [Building Insights · Landlord 탭 서비스]
     * @author 김민호
     * @description: 최근 3년 HPD 위반의 평균 수리일로 집주인 배지를 계산합니다 (closed 위반 3건 미만이면 Not enough data).
     *               Slow to fix면 Follow-up timer를 72h -> 48h로 줄입니다.
     *
     * @request userId
     * @return LandlordInsightResponse(trackRecord, followUpTimer)
     */
    LandlordInsightResponse getLandlordInsight(Long userId);

    /**
     * [Building Insights · Records 탭 서비스]
     * @author 김민호
     * @description: 최근 3년 HPD 민원·위반 요약, 카테고리별 민원, 반복 민원, 월별 민원 추세를 줍니다 (기록 3건 미만이면 Not enough data).
     *
     * @request userId
     * @return RecordsInsightResponse
     */
    RecordsInsightResponse getRecordsInsight(Long userId);

    /**
     * [HPD History 서비스]
     * @author 김민호
     * @description: OVERVIEW면 클래스별 위반·분기별 차트·요약, COMPLAINTS / VIOLATIONS면 최근 3년 기록 목록(최신 순)을 줍니다.
     *
     * @request userId, tab, page, size
     * @return HpdHistoryResponse
     */
    HpdHistoryResponse getHpdHistory(Long userId, HpdHistoryTab tab, int page, int size);
}
