package com.dwell.domain.user.controller;

import com.dwell.domain.user.dto.request.UserProfileUpdateRequest;
import com.dwell.domain.user.dto.request.UserSignUpRequest;
import com.dwell.domain.user.dto.response.UserProfileResponse;
import com.dwell.domain.user.dto.response.UserSignUpResponse;
import com.dwell.domain.user.service.UserService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @Operation(summary = "Sign up", description = "Registers a user with account info and home address.")
    @PostMapping("/user/signup")
    public ResponseEntity<BaseResponse<UserSignUpResponse>> signUp(
            @Valid @RequestBody UserSignUpRequest request) {

        // service 호출
        UserSignUpResponse response = userService.signUp(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Sign-up completed.", response));
    }

    // 이메일 중복 조회
    @Operation(summary = "Check email availability", description = "Returns true if the email is not taken.")
    @GetMapping("/user/email-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkEmail(
            @RequestParam String email) {

        // service 호출
        boolean available = userService.isEmailAvailable(email);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }

    // 닉네임 중복 조회
    @Operation(summary = "Check nickname availability", description = "Returns true if the nickname is not taken.")
    @GetMapping("/user/nickname-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkNickname(
            @RequestParam String nickname) {

        // service 호출
        boolean available = userService.isNicknameAvailable(nickname);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }

    // 내 정보 조회
    @Operation(summary = "내 정보 조회 API", description = "My Page, Manage Profile 화면의 닉네임, 이메일, 선호 언어, 프로필 사진, 주소를 조회하는 API")
    @GetMapping("/user/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // service 호출
        UserProfileResponse response = userService.getMyProfile(customUserDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Profile retrieved successfully.", response));
    }

    // 내 정보 수정
    @Operation(summary = "내 정보 수정 API", description = "Manage Profile 화면에서 닉네임, 선호 언어, 프로필 사진을 수정하는 API. 사진은 POST /files(purpose=PROFILE_IMAGE)로 먼저 올리고, 이전 사진은 삭제. 이메일은 수정 불가")
    @PatchMapping("/user/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        // service 호출
        UserProfileResponse response = userService.updateMyProfile(customUserDetails.getUser().getId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Profile updated successfully.", response));
    }

    // 회원 탈퇴
    @Operation(summary = "회원 탈퇴 API", description = "케이스, 증거, 파일(S3 포함), 채팅, 리프레시 토큰을 영구 삭제하는 API)커뮤니티 글과 댓글은 작성자를 Deleted user로 바꿔 유지, 데모 계정은 탈퇴 불가(403))")
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // service 호출
        userService.deleteMyAccount(customUserDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.noContent().build();
    }
}
