package com.dwell.domain.user.dto.request;

import com.dwell.domain.building.enums.UsState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "UserSignUpRequest DTO", description = "Sign-up request (account info + address)")
public class UserSignUpRequest {

    @Schema(description = "Nickname", example = "kjm")
    @NotBlank(message = "A nickname is a required field.")
    @Size(max = 30, message = "Nickname must be 30 characters or less.")
    private String nickname;

    @Schema(description = "Password (6-8 characters)", example = "abc123")
    @NotBlank(message = "A password is a required field.")
    @Size(min = 6, max = 8, message = "Password must be 6-8 characters long.")
    private String password;

    @Schema(description = "Password confirmation", example = "abc123")
    @NotBlank(message = "A password confirmation is a required field.")
    private String passwordConfirm;

    @Schema(description = "Email", example = "test01@gmail.com")
    @NotBlank(message = "Email is a required field.")
    @Email(message = "The email format is invalid.")
    private String email;

    @Schema(description = "NYC Borough-Block-Lot from address autocomplete", example = "1018800031")
    @NotBlank(message = "BBL is a required field.")
    @Pattern(regexp = "^\\d{10}$", message = "BBL must be 10 digits.")
    private String bbl; // 프론트 주소 자동완성 결과에서 받은 BBL (뉴욕시 필지 번호)

    @Schema(description = "Address line 1", example = "254 W 107th St")
    @NotBlank(message = "Address line 1 is a required field.")
    @Size(max = 255, message = "Address line 1 must be 255 characters or less.")
    private String street;

    @Schema(description = "Apt, unit, etc. (optional)", example = "Apt 4B")
    @Size(max = 20, message = "Apt/unit must be 20 characters or less.")
    private String unit;

    @Schema(description = "City", example = "New York")
    @NotBlank(message = "City is a required field.")
    @Size(max = 50, message = "City must be 50 characters or less.")
    private String city;

    @Schema(description = "State code", example = "NY")
    @NotNull(message = "State is a required field.")
    private UsState state;

    @Schema(description = "ZIP code", example = "10025")
    @NotBlank(message = "ZIP code is a required field.")
    @Pattern(regexp = "^\\d{5}$", message = "ZIP code must be 5 digits.")
    private String zip;
}
