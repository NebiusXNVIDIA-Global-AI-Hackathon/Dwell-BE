package com.dwell.domain.cases.controller;

import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.service.CaseService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Case", description = "케이스 관련 API")
public class CaseController {

    private final CaseService caseService;

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
}
