package com.dwell.domain.cases.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "ChecklistResponse: 케이스 체크리스트 응답 DTO")
public class ChecklistResponse {

    @Schema(description = "체크리스트 항목 (sortOrder 순)")
    private List<ChecklistItemResponse> items;
}
