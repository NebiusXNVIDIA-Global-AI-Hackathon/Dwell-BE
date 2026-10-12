package com.dwell.domain.file.dto.response;

import com.dwell.global.common.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "파일 업로드 응답 DTO", description = "업로드한 파일 정보 (케이스 생성 시 fileId 사용)")
public class FileUploadResponse {

    @Schema(description = "파일 ID (케이스 생성 요청의 fileIds에 사용)", example = "101")
    private Long fileId;

    @Schema(description = "파일 종류", example = "PHOTO")
    private MediaType mediaType;

    @Schema(description = "MIME 타입", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "파일 크기 (byte)", example = "2048576")
    private long sizeBytes;
}
