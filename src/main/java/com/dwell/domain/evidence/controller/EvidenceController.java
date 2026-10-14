package com.dwell.domain.evidence.controller;

import com.dwell.domain.evidence.dto.response.EvidenceDetailResponse;
import com.dwell.domain.evidence.service.EvidenceService;
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
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence", description = "증거 관련 API")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @Operation(summary = "증거 상세 조회 API", description = "증거 1건의 원본 파일(presigned URL)·SHA-256·AI 개별 판정(이슈 유형·심각도·근거·진행)을 조회하는 API. 분석 전이면 analysis = null")
    @GetMapping("/{evidenceId}")
    public ResponseEntity<BaseResponse<EvidenceDetailResponse>> getEvidence(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long evidenceId
    ) {
        EvidenceDetailResponse response = evidenceService.getEvidence(evidenceId, customUserDetails.getUser().getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "Evidence retrieved successfully.", response));
    }
}
