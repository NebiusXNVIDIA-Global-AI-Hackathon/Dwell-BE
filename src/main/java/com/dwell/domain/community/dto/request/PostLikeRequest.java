package com.dwell.domain.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "PostLikeRequest DTO", description = "좋아요 / 좋아요 취소 요청")
public class PostLikeRequest {

    @NotNull
    @Schema(description = "true = 좋아요, false = 취소 (같은 값을 여러 번 보내도 결과 같음)", example = "true")
    private Boolean liked;
}
