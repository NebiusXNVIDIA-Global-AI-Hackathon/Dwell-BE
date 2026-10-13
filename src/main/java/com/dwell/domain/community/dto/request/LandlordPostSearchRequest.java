package com.dwell.domain.community.dto.request;

import com.dwell.domain.community.enums.LandlordTopic;
import com.dwell.domain.community.enums.PostSort;
import com.dwell.domain.community.enums.PostedWithin;
import com.dwell.global.common.enums.FloorBand;
import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "LandlordPostSearchRequest DTO", description = "Posts about landlord 검색·필터 (쿼리 파라미터)")
public class LandlordPostSearchRequest {

    @Schema(description = "제목·내용 검색", example = "plumber")
    private String q;

    @Schema(description = "분류 (여러 개 가능)", example = "SLOW_REPAIRS")
    private List<LandlordTopic> topic;

    @Schema(description = "이슈 (WATER_FLOODING / HEAT_HOT_WATER / PESTS / MOLD_MOISTURE 등, 여러 개 가능)", example = "WATER_FLOODING")
    private List<IssueCategory> issueType;

    @Schema(description = "층 구간 (여러 개 가능)", example = "MIDDLE")
    private List<FloorBand> floorBand;

    @Schema(description = "게시 기간", example = "LAST_6_MONTHS")
    private PostedWithin posted;

    @Schema(description = "정렬 (기본 NEWEST)", example = "NEWEST")
    private PostSort sort = PostSort.NEWEST;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기 (최대 50)", example = "20")
    private int size = 20;
}
