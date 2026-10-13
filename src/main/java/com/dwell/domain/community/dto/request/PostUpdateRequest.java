package com.dwell.domain.community.dto.request;

import com.dwell.domain.community.enums.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "PostUpdateRequest DTO", description = "게시물 수정 요청 (보낸 필드만 수정, null이면 그대로)")
public class PostUpdateRequest {

    @Schema(description = "게시판 (FREE / TIPS)", example = "TIPS")
    private Board board;

    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Schema(description = "제목 (최대 100자)", example = "Pipe burst in my unit — fixed")
    private String title;

    @Size(max = 1000)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Schema(description = "내용 (최대 1000자)", example = "The super replaced the pipe under the sink. Took 2 days.")
    private String content;

    @Schema(description = "태그 전체 (최대 5개, 보내면 통째로 교체, []이면 전부 삭제)", example = "[\"pipeburst\", \"fixed\"]")
    private List<@Size(max = 30) String> tags;

    @Schema(description = "이미지 파일 ID 전체 (보내면 통째로 교체·순서 반영, []이면 전부 삭제, 빠진 이미지는 파일도 삭제)", example = "[]")
    private List<Long> imageFileIds;
}
