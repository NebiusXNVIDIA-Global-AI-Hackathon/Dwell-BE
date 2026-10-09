package com.dwell.domain.user.controller;

import com.dwell.domain.user.dto.request.UserSignUpRequest;
import com.dwell.domain.user.dto.response.UserSignUpResponse;
import com.dwell.domain.user.service.UserService;
import com.dwell.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @Operation(summary = "Sign up", description = "Registers a user with account info and home address.")
    @PostMapping("/auth/signup")
    public ResponseEntity<BaseResponse<UserSignUpResponse>> signUp(
            @Valid @RequestBody UserSignUpRequest request) {

        // service 호출
        UserSignUpResponse response = userService.signUp(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Sign-up completed.", response));
    }

    // 이메일 중복 조회
    @Operation(summary = "Check email availability", description = "Returns true if the email is not taken.")
    @GetMapping("/auth/email-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkEmail(
            @RequestParam String email) {

        // service 호출
        boolean available = userService.isEmailAvailable(email);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }

    // 닉네임 중복 조회
    @Operation(summary = "Check nickname availability", description = "Returns true if the nickname is not taken.")
    @GetMapping("/auth/nickname-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkNickname(
            @RequestParam String nickname) {

        // service 호출
        boolean available = userService.isNicknameAvailable(nickname);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }
}
