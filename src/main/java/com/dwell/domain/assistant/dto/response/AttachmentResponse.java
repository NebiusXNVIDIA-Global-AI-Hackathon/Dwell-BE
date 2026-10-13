package com.dwell.domain.assistant.dto.response;

import com.dwell.global.common.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "AttachmentResponse DTO", description = "첨부 파일")
public class AttachmentResponse {

    @Schema(description = "파일 ID", example = "130")
    private Long fileId;

    @Schema(description = "파일 종류", example = "PHOTO")
    private MediaType mediaType;

    @Schema(description = "파일 URL (10분 동안 유효한 presigned URL)", example = "https://...")
    private String url;
}
