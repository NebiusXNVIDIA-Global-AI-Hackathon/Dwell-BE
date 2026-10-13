package com.dwell.domain.building.dto.response;

import com.dwell.domain.community.dto.response.PostSummaryResponse;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "SignalDetailResponse DTO", description = "Issue Details (시그널 상세)")
public class SignalDetailResponse {

    @Schema(description = "시그널 ID", example = "12")
    private Long signalId;

    @Schema(description = "제목", example = "Water leak")
    private String title;

    @Schema(description = "이슈 카테고리", example = "WATER_FLOODING")
    private IssueCategory issueCategory;

    @Schema(description = "내 진행 중 케이스와 같은 이슈인지", example = "true")
    private boolean relatedToMyCase;

    @Schema(description = "Deaver's insight (en / native, 없으면 null)")
    private LocalizedText insight;

    @Schema(description = "Neighbor reports: 이슈 관련 태그가 달린 내 건물 게시글 (좋아요 많은 순 5개, 카드 형식은 게시물 목록과 같음)")
    private List<PostSummaryResponse> posts;
}
