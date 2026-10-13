package com.dwell.domain.community.dto.request;

import com.dwell.domain.community.enums.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "PostCreateRequest DTO", description = "게시물 작성 요청")
public class PostCreateRequest {

    @NotNull
    @Schema(description = "게시판 (FREE / TIPS)", example = "FREE")
    private Board board;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "제목 (최대 100자)", example = "Pipe burst in my unit")
    private String title;

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "내용 (최대 1000자)", example = "Water is coming out from under the kitchen sink and spreading to the hallway. Super is on the way.")
    private String content;

    @Schema(description = "태그 (최대 5개, 없는 태그는 새로 생성)", example = "[\"pipeburst\", \"kitchen\", \"waterpipe\"]")
    private List<@Size(max = 30) String> tags;

    @Schema(description = "이미지 파일 ID 목록 (POST /files, purpose=POST_IMAGE)", example = "[]")
    private List<Long> imageFileIds;
}
