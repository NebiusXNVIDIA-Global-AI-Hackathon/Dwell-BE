package com.dwell.domain.admin.controller;

import com.dwell.domain.admin.dto.request.DemoTimeShiftRequest;
import com.dwell.domain.admin.dto.response.JobRunResponse;
import com.dwell.domain.admin.service.AdminJobService;
import com.dwell.domain.admin.service.DemoTimeShiftService;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 전용 (SecurityConfig: /api/v1/admin/** = ROLE_ADMIN)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 · 데모 API (ADMIN 권한 필요)")
public class AdminController {

    private final AdminJobService adminJobService;
    private final DemoTimeShiftService demoTimeShiftService;

    @Operation(summary = "배치 수동 실행 API", description = "Job을 즉시 실행하는 API. 지금은 sla-check(응답 기한 경과·수리 당일 전이)만 지원")
    @PostMapping("/jobs/{jobName}/run")
    public ResponseEntity<BaseResponse<JobRunResponse>> runJob(
            @PathVariable String jobName
    ) {
        JobRunResponse response = adminJobService.runJob(jobName);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(BaseResponse.success(202, "Job executed successfully.", response));
    }

    @Operation(summary = "데모 시간 이동 API", description = "케이스의 모든 시각을 hours만큼 과거로 옮겨 시간이 흐른 상태를 만들고, SLA 체크를 즉시 실행한 뒤 갱신된 케이스 상세를 반환하는 API (데모 서버에서만 동작)")
    @PostMapping("/demo/cases/{caseId}/time-shift")
    public ResponseEntity<BaseResponse<CaseOverviewResponse>> shiftCaseTime(
            @PathVariable Long caseId,
            @Valid @RequestBody DemoTimeShiftRequest request
    ) {
        CaseOverviewResponse response = demoTimeShiftService.shiftCase(caseId, request.getHours());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.success(200, "Case time shifted successfully.", response));
    }
}
