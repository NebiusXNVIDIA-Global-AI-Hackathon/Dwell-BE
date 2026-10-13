package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "LandlordPostListResponse DTO", description = "Posts about landlord (페이지)")
public class LandlordPostListResponse {

    @Schema(description = "건물 주소 (호수 제외)", example = "312 E 21st St")
    private String address;

    @Schema(description = "칩 숫자 (ALL / SLOW_REPAIRS / NO_REPLY / FIXED_WELL, topic 외 필터를 적용한 기준)")
    private Map<String, Long> topicCounts;

    @Schema(description = "글 목록")
    private List<LandlordPostResponse> content;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 글 수 (N posts match)", example = "2")
    private long totalElements;

    @Schema(description = "다음 페이지 여부", example = "false")
    private boolean hasNext;
}
