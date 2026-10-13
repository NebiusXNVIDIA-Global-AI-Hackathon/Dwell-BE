package com.dwell.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "내 정보 응답 DTO", description = "My Page, Manage Profile 화면의 내 정보")
public class UserProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일 (수정 불가)", example = "test01@gmail.com")
    private String email;

    @Schema(description = "닉네임", example = "seohu")
    private String nickname;

    @Schema(description = "선호 언어 (en, zh, es)", example = "es")
    private String preferredLanguage;

    @Schema(description = "프로필 사진 URL", example = "https://dwell-bucket.s3.amazonaws.com/profile_image/1/...")
    private String profileImageUrl;

    @Schema(description = "집 주소 (주소 1줄, 호수)", example = "254 W 107th St, Apt 4B")
    private String address;

    @Schema(description = "집 정보 등록 여부 (UserPlace가 있으면 true)", example = "true")
    private boolean onboarded;
}
