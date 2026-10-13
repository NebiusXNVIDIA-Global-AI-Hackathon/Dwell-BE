package com.dwell.domain.cases.controller;

import com.dwell.domain.cases.dto.request.CaseCreateRequest;
import com.dwell.domain.cases.dto.request.CaseEventCreateRequest;
import com.dwell.domain.cases.dto.request.CaseEvidenceAddRequest;
import com.dwell.domain.cases.dto.response.CaseCreateResponse;
import com.dwell.domain.cases.dto.response.CaseOptionsResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

    // 케이스 입력 옵션
    @Operation(summary = "케이스 입력 옵션 API", description = "New Case 선택지(이슈 카테고리·세부 이슈·공간·부위), 안전 경고 문구, 증거 촬영 가이드를 조회하는 API")
    @GetMapping("/options")
    public ResponseEntity<BaseResponse<CaseOptionsResponse>> getCaseOptions() {

        // service 호출
        CaseOptionsResponse response = caseService.getCaseOptions();

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Case options retrieved successfully.", response));
    }

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

    // 케이스 생성
    @Operation(summary = "케이스 생성 API", description = "New Case 선택값과 POST /files로 올린 파일로 케이스를 생성하는 API. 파일은 증거 1건으로 묶이고, 생성 직후 stage는 LOGGED, AI 분석 상태는 PENDING")
    @PostMapping
    public ResponseEntity<BaseResponse<CaseCreateResponse>> createCase(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CaseCreateRequest request) {

        // service 호출
        CaseCreateResponse response = caseService.createCase(customUserDetails.getUser().getId(), request);

        // 응답 반환 (AI 분석은 비동기라 202)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BaseResponse.success(202, "Case created. AI analysis has started.", response));
    }

    // 케이스 증거 추가
    @Operation(summary = "케이스 증거 추가 API", description = "POST /files로 올린 파일을 기존 케이스의 증거 1건으로 추가하고, 갱신된 케이스 상세를 반환하는 API. 추가 후 AI 분석 상태는 PENDING")
    @PostMapping("/{caseId}/evidence")
    public ResponseEntity<BaseResponse<CaseOverviewResponse>> addEvidence(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long caseId,
            @Valid @RequestBody CaseEvidenceAddRequest request) {

        // service 호출
        CaseOverviewResponse response = caseService.addEvidence(caseId, customUserDetails.getUser().getId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Evidence added successfully.", response));
    }

    // 케이스 삭제
    @Operation(summary = "케이스 삭제 API", description = "케이스와 연결된 증거,파일,타임라인,체크리스트,Notice,검증,케이스 채팅을 영구 삭제하는 API(S3 원본 파일도 삭제)")
    @DeleteMapping("/{caseId}")
    public ResponseEntity<Void> deleteCase(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long caseId) {

        // service 호출
        caseService.deleteCase(caseId, customUserDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.noContent().build();
    }
}
