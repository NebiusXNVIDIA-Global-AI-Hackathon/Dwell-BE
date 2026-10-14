package com.dwell.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "토큰 재발급 응답 DTO", description = "새 액세스 토큰과 새 리프레시 토큰 (기존 리프레시 토큰은 폐기)")
public class RefreshTokenResponse {

    @Schema(description = "새 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "새 리프레시 토큰 (기존 토큰 대신 저장)", example = "Xk3p9Qm2...")
    private String refreshToken;
}
