package com.dwell.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "로그인 응답 DTO", description = "로그인 성공 시 반환하는 데이터")
public class LoginResponse {

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "리프레시 토큰 (14일, 액세스 토큰이 만료되면 POST /auth/refresh로 재발급)", example = "Xk3p9Qm2...")
    private String refreshToken;

    @Schema(description = "로그인한 사용자 정보")
    private UserInfo user;

    @Getter
    @Builder
    @Schema(title = "로그인 사용자 정보")
    public static class UserInfo {

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "닉네임", example = "kjm")
        private String nickname;
    }
}
