package com.dwell.domain.auth.controller;

import com.dwell.domain.auth.dto.request.LoginRequest;
import com.dwell.domain.auth.dto.request.RefreshTokenRequest;
import com.dwell.domain.auth.dto.response.LoginResponse;
import com.dwell.domain.auth.dto.response.RefreshTokenResponse;
import com.dwell.domain.auth.service.AuthService;
import com.dwell.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 액세스 토큰과 리프레시 토큰을 발급하는 API")
    @PostMapping("/auth/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        // service 호출
        LoginResponse response = authService.login(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Login completed.", response));
    }

    // 데모 계정 로그인
    @Operation(summary = "데모 계정 로그인", description = "회원가입 없이 데모 계정으로 로그인하고 액세스 토큰과 리프레시 토큰을 발급하는 API")
    @PostMapping("/auth/demo")
    public ResponseEntity<BaseResponse<LoginResponse>> demoLogin() {

        // service 호출
        LoginResponse response = authService.demoLogin();

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Demo login completed.", response));
    }

    // 토큰 재발급
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새 액세스 토큰과 새 리프레시 토큰을 발급하는 API. 사용한 리프레시 토큰은 폐기되므로 응답의 새 토큰으로 바꿔 저장")
    @PostMapping("/auth/refresh")
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        // service 호출
        RefreshTokenResponse response = authService.refresh(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Token refreshed.", response));
    }
}
