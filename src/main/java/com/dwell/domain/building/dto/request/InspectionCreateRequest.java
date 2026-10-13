package com.dwell.domain.building.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "InspectionCreateRequest DTO", description = "Check My Unit 결과 저장 요청")
public class InspectionCreateRequest {

    @NotNull
    @Schema(description = "시그널 ID", example = "14")
    private Long signalId;

    @NotEmpty
    @Schema(description = "체크한 증상 키 (1개 이상)", example = "[\"ROACHES_SEEN\", \"DROPPINGS\"]")
    private List<String> symptoms;
}
