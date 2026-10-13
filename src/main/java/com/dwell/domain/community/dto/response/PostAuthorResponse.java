package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PostAuthorResponse DTO", description = "게시물 작성자 (층 구간만 노출)")
public class PostAuthorResponse {

    @Schema(description = "표시 이름 (탈퇴한 작성자는 Deleted user)", example = "Middle Floor neighbor")
    private String label;

    @Schema(description = "내 게시물 여부 (true면 ⋯ 수정·삭제 메뉴 노출)", example = "true")
    private Boolean isMe; // boolean이면 Lombok getter가 isMe()라 JSON 키가 "me"가 됨
}
