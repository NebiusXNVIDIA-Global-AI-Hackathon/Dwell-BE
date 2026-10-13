package com.dwell.domain.community.dto.response;

import com.dwell.global.common.enums.FloorBand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ReplyAuthorResponse DTO", description = "댓글 작성자 (닉네임 + 층 구간)")
public class ReplyAuthorResponse {

    @Schema(description = "닉네임 (탈퇴한 작성자는 Deleted user)", example = "GuiltyMo")
    private String nickname;

    @Schema(description = "아바타 이니셜", example = "GM")
    private String initials;

    @Schema(description = "층 구간 (LOWER / MIDDLE / UPPER)", example = "LOWER")
    private FloorBand floorBand;

    @Schema(description = "내 댓글 여부", example = "false")
    private Boolean isMe; // boolean이면 Lombok getter가 isMe()라 JSON 키가 "me"가 됨
}
