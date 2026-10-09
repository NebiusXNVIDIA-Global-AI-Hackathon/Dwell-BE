package com.dwell.domain.user.dto.response;

import com.dwell.domain.building.enums.UsState;
import com.dwell.domain.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "UserSignUpResponse DTO", description = "Sign-up result")
public class UserSignUpResponse {

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "Email", example = "test01@gmail.com")
    private String email;

    @Schema(description = "Nickname", example = "kjm")
    private String nickname;

    @Schema(description = "Role", example = "USER")
    private Role role;

    @Schema(description = "Address line 1", example = "254 W 107th St")
    private String street;

    @Schema(description = "Apt, unit, etc.", example = "Apt 4B")
    private String unit;

    @Schema(description = "City", example = "New York")
    private String city;

    @Schema(description = "State code", example = "NY")
    private UsState state;

    @Schema(description = "ZIP code", example = "10025")
    private String zip;

    @Schema(description = "Created at", example = "2026-10-09T12:00:00")
    private LocalDateTime createdAt;
}
