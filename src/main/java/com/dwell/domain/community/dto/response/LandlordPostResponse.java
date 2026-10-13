package com.dwell.domain.community.dto.response;

import com.dwell.domain.community.enums.Board;
import com.dwell.domain.community.enums.LandlordTopic;
import com.dwell.global.common.enums.FloorBand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "LandlordPostResponse DTO", description = "집주인 언급 글 카드")
public class LandlordPostResponse {

    @Schema(description = "게시물 ID", example = "41")
    private Long postId;

    @Schema(description = "게시판 (FREE / TIPS)", example = "FREE")
    private Board board;

    @Schema(description = "작성자 층 구간", example = "MIDDLE")
    private FloorBand floorBand;

    @Schema(description = "제목", example = "Landlord took 3 weeks to send a plumber")
    private String title;

    @Schema(description = "내용 미리보기", example = "Asked twice in writing before anyone came.")
    private String preview;

    @Schema(description = "분류 (키워드로 판단, 어디에도 안 맞으면 null)", example = "SLOW_REPAIRS")
    private LandlordTopic landlordTopic;

    @Schema(description = "태그 (#포함)", example = "[\"#waterleak\"]")
    private List<String> tags;

    @Schema(description = "좋아요 수", example = "14")
    private long likeCount;

    @Schema(description = "댓글 수", example = "6")
    private long replyCount;

    @Schema(description = "작성 시각", example = "2026-10-02T10:00:00")
    private LocalDateTime createdAt;
}
