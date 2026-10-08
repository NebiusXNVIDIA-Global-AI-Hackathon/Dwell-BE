package com.dwell.domain.file.entity;

import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.enums.MediaType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Uploaded file metadata (the binary lives in Object Storage)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. f_8a1c
    @Column(nullable = false, unique = true, length = 20)
    private String publicId;

    // Uploader
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // NULL until the evidence is registered
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id")
    private Evidence evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FilePurpose purpose = FilePurpose.EVIDENCE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType mediaType;

    @Column(nullable = false, length = 50)
    private String mimeType;

    @Column(nullable = false)
    private long sizeBytes;

    // VIDEO at least 5s, AUDIO at most 180s
    private Integer durationSec;

    @Column(nullable = false)
    private String storageKey;

    // Original integrity check for the evidence package
    @Column(nullable = false, length = 64)
    private String sha256;

    // From EXIF
    private LocalDateTime capturedAt;

    // Quality-check result
    private Boolean qualityOk;

    @Builder
    public File(String publicId, User user, FilePurpose purpose, MediaType mediaType, String mimeType,
                long sizeBytes, Integer durationSec, String storageKey, String sha256, LocalDateTime capturedAt) {
        this.publicId = publicId;
        this.user = user;
        if (purpose != null) {
            this.purpose = purpose;
        }
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.durationSec = durationSec;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.capturedAt = capturedAt;
    }
}
