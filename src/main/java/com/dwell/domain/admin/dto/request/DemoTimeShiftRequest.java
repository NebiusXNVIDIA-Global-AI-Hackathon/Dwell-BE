package com.dwell.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(title = "DemoTimeShiftRequest: 데모 시간 이동 요청 DTO")
public class DemoTimeShiftRequest {

    @NotNull
    @Min(1)
    @Max(720)
    @Schema(description = "앞당길 시간 (1~720시간). 케이스의 모든 시각이 이만큼 과거로 이동 = 그만큼 시간이 흐른 상태", example = "24")
    private Integer hours;
}
