package com.dwell.domain.cases.controller;

import com.dwell.domain.cases.dto.request.CaseEventCreateRequest;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.dto.response.CaseTimelineResponse;
import com.dwell.domain.cases.service.CaseEventService;
import com.dwell.domain.cases.service.CaseService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Case", description = "케이스 관련 API")
public class CaseController {

    private final CaseService caseService;
    private final CaseEventService caseEventService;

    @Operation(summary = "케이스 상세 조회 API", description = "케이스 ID 기반으로, 헤더·Current Status 배너·진행 스텝·체크리스트·증거 요약을 조회하는 API")
    @GetMapping("/{caseId}")
    public ResponseEntity<BaseResponse<CaseOverviewResponse>> getCaseOverview(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long caseId
    ) {
        CaseOverviewResponse response = caseService.getCaseOverview(caseId, customUserDetails.getUser().getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "Case overview retrieved successfully.", response));
    }

    @Operation(summary = "케이스 타임라인 조회 API", description = "케이스 ID 기반으로, 헤더와 전체 이벤트 카드(제목·설명·관련 증거·경고 표시)를 오래된 순으로 조회하는 API")
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<BaseResponse<CaseTimelineResponse>> getCaseTimeline(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long caseId
    ) {
        CaseTimelineResponse response = caseService.getCaseTimeline(caseId, customUserDetails.getUser().getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "Case timeline retrieved successfully.", response));
    }

    @Operation(summary = "케이스 상태 업데이트 API", description = "케이스에 이벤트(답장·일정·311 신고 등)를 기록하고 stage를 전이한 뒤, 갱신된 케이스 상세를 반환하는 API")
    @PostMapping("/{caseId}/events")
    public ResponseEntity<BaseResponse<CaseOverviewResponse>> recordCaseEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long caseId,
            @Valid @RequestBody CaseEventCreateRequest request
    ) {
        CaseOverviewResponse response = caseEventService.recordEvent(caseId, customUserDetails.getUser().getId(), request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "Case event recorded successfully.", response));
    }
}
