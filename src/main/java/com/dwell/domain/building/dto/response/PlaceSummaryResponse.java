package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "PlaceSummaryResponse DTO", description = "내 건물 요약 (My Place 메인)")
public class PlaceSummaryResponse {

    @Schema(description = "주소 (호수 포함)", example = "245 W 107th St, Apt 4B, New York, NY 10025")
    private String address;

    @Schema(description = "NYC Borough-Block-Lot", example = "1018790031")
    private String bbl;

    @Schema(description = "Rising Issue 카드 (Building Signals의 Rising Issue와 같은 목록, 없으면 [] -> All clear!!!)")
    private List<SignalItemResponse> risingIssues;
}
