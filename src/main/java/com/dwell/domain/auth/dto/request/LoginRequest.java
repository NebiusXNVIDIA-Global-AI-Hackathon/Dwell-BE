package com.dwell.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "로그인 요청 DTO", description = "이메일과 비밀번호로 로그인할 때 서버에 보내는 데이터")
public class LoginRequest {

    @Schema(description = "이메일", example = "test01@gmail.com")
    @NotBlank(message = "Email is a required field.")
    @Email(message = "The email format is invalid.")
    private String email;

    @Schema(description = "비밀번호", example = "abc123")
    @NotBlank(message = "A password is a required field.")
    private String password;
}