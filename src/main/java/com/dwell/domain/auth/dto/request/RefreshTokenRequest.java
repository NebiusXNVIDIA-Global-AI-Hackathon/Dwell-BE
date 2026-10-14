package com.dwell.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "토큰 재발급 요청 DTO", description = "로그인 때 받은 리프레시 토큰")
public class RefreshTokenRequest {

    @Schema(description = "리프레시 토큰", example = "Xk3p9Qm2...")
    @NotBlank(message = "Refresh token is a required field.")
    private String refreshToken;
}
