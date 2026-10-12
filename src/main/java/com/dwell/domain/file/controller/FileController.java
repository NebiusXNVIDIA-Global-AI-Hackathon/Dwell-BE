package com.dwell.domain.file.controller;

import com.dwell.domain.file.dto.response.FileUploadResponse;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.service.FileService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "파일 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // 파일 업로드
    @Operation(summary = "파일 업로드 API", description = "파일(사진·영상·음성·문서)을 S3에 올리고 fileId를 반환하는 API. purpose를 생략하면 EVIDENCE (케이스 생성 요청의 fileIds에 사용)")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadFile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "purpose", defaultValue = "EVIDENCE") FilePurpose purpose) {

        // service 호출
        FileUploadResponse response = fileService.uploadFile(customUserDetails.getUser().getId(), file, purpose);

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "File uploaded successfully.", response));
    }
}
