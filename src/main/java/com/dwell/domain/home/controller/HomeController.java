package com.dwell.domain.home.controller;

import com.dwell.domain.home.dto.response.HomeResponse;
import com.dwell.domain.home.service.HomeService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    // 홈 화면 조회
    @Operation(summary = "홈 화면 API", description = "홈 화면 데이터를 한 번에 주는 API. 사용자 닉네임, 진행 중 케이스 카드(미니 타임라인·체크리스트, 응답 기한 임박 → 최근 업데이트 순), Needs Your Attention 카드")
    @GetMapping("/home")
    public ResponseEntity<BaseResponse<HomeResponse>> getHome(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // service 호출
        HomeResponse response = homeService.getHome(customUserDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Home retrieved successfully.", response));
    }
}
