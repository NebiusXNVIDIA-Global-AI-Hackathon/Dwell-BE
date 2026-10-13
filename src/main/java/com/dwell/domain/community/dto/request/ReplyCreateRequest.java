package com.dwell.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "ReplyCreateRequest DTO", description = "댓글 작성 요청")
public class ReplyCreateRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "내용 (1~500자)", example = "Same here. Ours started after the repair too.")
    private String content;
}
