package com.dwell.domain.building.dto.request;

import com.dwell.domain.building.enums.UsState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "집 주소 변경 요청 DTO", description = "My Home 화면의 주소 (자동완성으로 고른 주소의 BBL 포함)")
public class PlaceUpdateRequest {

    @Schema(description = "주소 1줄", example = "254 W 107th St")
    @NotBlank(message = "Address line 1 is a required field.")
    @Size(max = 255, message = "Address line 1 must be 255 characters or less.")
    private String street;

    @Schema(description = "호수 (선택)", example = "Apt 4B")
    @Size(max = 20, message = "Apt/unit must be 20 characters or less.")
    private String unit;

    @Schema(description = "도시", example = "New York")
    @NotBlank(message = "City is a required field.")
    @Size(max = 50, message = "City must be 50 characters or less.")
    private String city;

    @Schema(description = "주 코드", example = "NY")
    @NotNull(message = "State is a required field.")
    private UsState state;

    @Schema(description = "ZIP 코드", example = "10025")
    @NotBlank(message = "ZIP code is a required field.")
    @Pattern(regexp = "^\\d{5}$", message = "ZIP code must be 5 digits.")
    private String zip;

    @Schema(description = "주소 자동완성 결과의 BBL (뉴욕시 필지 번호)", example = "1018790031")
    @NotBlank(message = "BBL is a required field.")
    @Pattern(regexp = "^\\d{10}$", message = "BBL must be 10 digits.")
    private String bbl;
}
