package com.dwell.domain.file.service;

import com.dwell.domain.file.dto.response.FileUploadResponse;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // 파일 업로드 (케이스·게시글 등에 연결되기 전 상태로 저장)
    @Transactional
    public FileUploadResponse uploadFile(Long userId, MultipartFile multipartFile, FilePurpose purpose) {

        // 빈 파일인지 확인
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("[FileService] File upload failed: empty file");
            throw new CustomException(FileErrorCode.EMPTY_FILE);
        }

        // 파일 종류 판단 (사진·영상·음성·문서만 허용)
        MediaType mediaType = toMediaType(multipartFile.getContentType());
        if (mediaType == null) {
            log.warn("[FileService] File upload failed: unsupported file type: contentType={}", multipartFile.getContentType());
            throw new CustomException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        // 프로필 사진·게시글 이미지는 사진만 허용
        if ((purpose == FilePurpose.PROFILE_IMAGE || purpose == FilePurpose.POST_IMAGE) && mediaType != MediaType.PHOTO) {
            log.warn("[FileService] File upload failed: only images are allowed: purpose={}, contentType={}", purpose, multipartFile.getContentType());
            throw new CustomException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        // 위변조 확인용 해시 계산 ("Protected from tampering")
        String sha256 = calculateSha256(multipartFile);

        // S3 업로드 (용도별·사용자별 폴더, 예: evidence/1, profile_image/1)
        String storageKey = s3Uploader.upload(multipartFile, purpose.name().toLowerCase() + "/" + userId);

        // 업로드한 사용자 (인증 필터에서 이미 확인했으므로 조회 없이 참조만)
        User user = userRepository.getReferenceById(userId);

        // 파일 객체 생성
        File file =
                File.builder()
                        .publicId("f_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                        .user(user)
                        .purpose(purpose)
                        .mediaType(mediaType)
                        .mimeType(multipartFile.getContentType())
                        .sizeBytes(multipartFile.getSize())
                        .storageKey(storageKey)
                        .sha256(sha256)
                        .build();

        // DB 저장
        File savedFile = fileRepository.save(file);

        // 로그 출력
        log.info("[FileService] File uploaded: fileId={}, mediaType={}", savedFile.getId(), mediaType);

        // 응답 세팅
        return FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .mediaType(savedFile.getMediaType())
                .mimeType(savedFile.getMimeType())
                .sizeBytes(savedFile.getSizeBytes())
                .build();
    }

    // ========== 헬퍼 함수 ==========

    // MIME 타입 → 파일 종류 (허용하지 않는 형식이면 null)
    private MediaType toMediaType(String contentType) {
        if (contentType == null) {
            return null;
        }
        if (contentType.startsWith("image/")) {
            return MediaType.PHOTO;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        if (contentType.equals("application/pdf")
                || contentType.equals("application/msword")
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return MediaType.DOCUMENT;
        }
        return null;
    }

    // 파일 내용의 SHA-256 해시 (16진수 64자)
    private String calculateSha256(MultipartFile multipartFile) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
