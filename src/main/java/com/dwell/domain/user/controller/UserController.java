package com.dwell.domain.user.controller;

import com.dwell.domain.community.dto.response.MyPostListResponse;
import com.dwell.domain.community.dto.response.MyReplyListResponse;
import com.dwell.domain.community.service.CommunityService;
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
    private final CommunityService communityService;

    // 회원가입
    @Operation(summary = "Sign up", description = "Registers a user with account info and home address.")
    @PostMapping("/users/signup")
    public ResponseEntity<BaseResponse<UserSignUpResponse>> signUp(
            @Valid @RequestBody UserSignUpRequest request) {

        // service 호출
        UserSignUpResponse response = userService.signUp(request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Sign-up completed.", response));
    }

    // 이메일 중복 조회
    @Operation(summary = "Check email availability", description = "Returns true if the email is not taken.")
    @GetMapping("/users/email-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkEmail(
            @RequestParam String email) {

        // service 호출
        boolean available = userService.isEmailAvailable(email);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }

    // 닉네임 중복 조회
    @Operation(summary = "Check nickname availability", description = "Returns true if the nickname is not taken.")
    @GetMapping("/users/nickname-availability")
    public ResponseEntity<BaseResponse<Boolean>> checkNickname(
            @RequestParam String nickname) {

        // service 호출
        boolean available = userService.isNicknameAvailable(nickname);

        // 응답 반환
        return ResponseEntity.ok(BaseResponse.success(available));
    }

    // 내 정보 조회
    @Operation(summary = "내 정보 조회 API", description = "My Page, Manage Profile 화면의 닉네임, 이메일, 선호 언어, 프로필 사진, 주소를 조회하는 API")
    @GetMapping("/users/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // service 호출
        UserProfileResponse response = userService.getMyProfile(customUserDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Profile retrieved successfully.", response));
    }

    // 내 정보 수정
    @Operation(summary = "내 정보 수정 API", description = "Manage Profile 화면에서 닉네임, 선호 언어, 프로필 사진을 수정하는 API. 사진은 POST /files(purpose=PROFILE_IMAGE)로 먼저 올리고, 이전 사진은 삭제. 이메일은 수정 불가")
    @PatchMapping("/users/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        // service 호출
        UserProfileResponse response = userService.updateMyProfile(customUserDetails.getUser().getId(), request);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "Profile updated successfully.", response));
    }

    // 내 게시물 목록
    @Operation(summary = "내 게시물 목록 API", description = "My Activities Posts 탭. 지금 내 건물에 쓴 게시물을 최신 순으로 조회하고, 탭 숫자(게시물 수, 댓글 수)를 함께 주는 API. 수정·삭제는 커뮤니티 API 사용")
    @GetMapping("/users/me/posts")
    public ResponseEntity<BaseResponse<MyPostListResponse>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // service 호출
        MyPostListResponse response = communityService.getMyPosts(customUserDetails.getUser().getId(), page, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "My posts retrieved successfully.", response));
    }

    // 내 댓글 목록
    @Operation(summary = "내 댓글 목록 API", description = "My Activities Replies 탭. 지금 내 건물 게시물에 단 댓글을 최신 순으로, 게시물 제목과 함께 조회하는 API")
    @GetMapping("/users/me/replies")
    public ResponseEntity<BaseResponse<MyReplyListResponse>> getMyReplies(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // service 호출
        MyReplyListResponse response = communityService.getMyReplies(customUserDetails.getUser().getId(), page, size);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(200, "My replies retrieved successfully.", response));
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
