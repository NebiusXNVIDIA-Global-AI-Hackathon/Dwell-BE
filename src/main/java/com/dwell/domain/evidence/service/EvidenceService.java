package com.dwell.domain.evidence.service;

import com.dwell.domain.evidence.dto.response.EvidenceDetailResponse;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.exception.EvidenceErrorCode;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;

    // 증거 1건 원본·해시·개별 판정
    public EvidenceDetailResponse getEvidence(Long evidenceId, Long userId) {

        log.info("[EvidenceService] 증거 상세 조회 - 시작: evidenceId={}", evidenceId);

        // 증거 조회 (삭제된 증거 제외)
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> {
                    log.warn("[EvidenceService] 증거 조회 실패 - 존재하지 않는 증거 evidenceId: {}", evidenceId);
                    return new CustomException(EvidenceErrorCode.EVIDENCE_NOT_FOUND);
                });

        // 본인의 증거가 아니면 X
        if (!evidence.getUser().getId().equals(userId)) {
            log.warn("[EvidenceService] 증거 조회 실패 - 타인 증거 접근 시도 evidenceId: {}, userId: {}", evidenceId, userId);
            throw new CustomException(EvidenceErrorCode.NOT_EVIDENCE_OWNER);
        }

        List<File> files = fileRepository.findAllByEvidenceInOrderByIdAsc(List.of(evidence));

        List<EvidenceDetailResponse.FileItem> fileItems = new ArrayList<>();
        for (File file : files) {
            fileItems.add(EvidenceDetailResponse.FileItem.builder()
                    .fileId(file.getPublicId())
                    .mediaType(file.getMediaType())
                    .url(s3Uploader.getPresignedUrl(file.getStorageKey()))
                    .sha256(file.getSha256())
                    .capturedAt(file.getCapturedAt())
                    .durationSec(file.getDurationSec())
                    .build());
        }

        // 분석이 끝난 경우에만 개별 판정
        EvidenceDetailResponse.Analysis analysis = null;
        if (evidence.getAnalysisStatus() == AnalysisStatus.DONE) {
            analysis = EvidenceDetailResponse.Analysis.builder()
                    .issueType(evidence.getIssueType())
                    .severity(evidence.getSeverity())
                    .reasons(evidence.getReasons() == null ? List.of() : evidence.getReasons())
                    .progression(evidence.getProgression())
                    .progressionReason(evidence.getProgressionReason())
                    .build();
        }

        boolean allHashed = !files.isEmpty() && files.stream().allMatch(file -> file.getSha256() != null && !file.getSha256().isBlank());

        EvidenceDetailResponse response = EvidenceDetailResponse.builder()
                .evidenceId(evidence.getId())
                .evidenceCode(evidence.getEvidenceCode())
                .title(evidence.getTitle())
                .caseId(evidence.getCaseEntity().getId())
                .mediaType(evidence.getMediaType())
                .mediaSummary(toMediaSummary(files))
                .phase(evidence.getPhase())
                .files(fileItems)
                .location(evidence.getLocation())
                .locationDetail(evidence.getLocationDetail())
                .subLocation(evidence.getSubLocation())
                .locationLabel(toLocationLabel(evidence))
                .description(evidence.getDescription())
                .createdAt(evidence.getCreatedAt())
                .analysisStatus(evidence.getAnalysisStatus())
                .analysis(analysis)
                .integrity(EvidenceDetailResponse.Integrity.builder()
                        .originalSaved(!files.isEmpty())
                        .aiAssessmentAttached(analysis != null)
                        .tamperProtected(allHashed)
                        .build())
                .build();

        log.info("[EvidenceService] 증거 상세 조회 - 종료: evidenceId={}, files={}", evidenceId, files.size());

        return response;
    }

    // ========== 헬퍼 함수 ==========

    // "1 photo · 1 audio" (와프 순서: 사진 → 영상 → 음성 → 문서)
    static String toMediaSummary(List<File> files) {
        Map<MediaType, Integer> counts = new EnumMap<>(MediaType.class);
        for (File file : files) {
            counts.merge(file.getMediaType(), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((type, count) -> {
            String name = type.name().toLowerCase(Locale.ROOT);
            parts.add(count + " " + (count > 1 && type != MediaType.AUDIO ? name + "s" : name));
        });
        return String.join(" · ", parts);
    }

    // "Bathroom · ceiling" (부위는 AI 추정값, 없으면 공간만)
    private static String toLocationLabel(Evidence evidence) {
        String place = evidence.getLocation() == Location.OTHER && evidence.getLocationDetail() != null
                ? evidence.getLocationDetail() : evidence.getLocation().getLabel();
        if (evidence.getSubLocation() == null || evidence.getSubLocation().isBlank()) {
            return place;
        }
        return place + " · " + evidence.getSubLocation();
    }
}
