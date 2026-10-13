package com.dwell.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "내 정보 수정 요청 DTO", description = "Manage Profile 화면의 닉네임, 선호 언어, 프로필 사진 (Save 시 폼 전체 전송)")
public class UserProfileUpdateRequest {

    @Schema(description = "닉네임", example = "seohu")
    @NotBlank(message = "A nickname is a required field.")
    @Size(max = 30, message = "Nickname must be 30 characters or less.")
    private String nickname;

    @Schema(description = "선호 언어 (en, zh, es)", example = "es")
    @NotBlank(message = "Preferred language is a required field.")
    @Pattern(regexp = "^(en|zh|es)$", message = "Preferred language must be en, zh, or es.")
    private String preferredLanguage;

    @Schema(description = "POST /files(purpose=PROFILE_IMAGE)로 올린 파일 ID (사진을 바꿀 때만, 비우면 지금 사진 유지)", example = "120")
    private Long profileImageFileId;
}
