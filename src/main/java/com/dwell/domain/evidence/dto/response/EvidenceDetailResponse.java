package com.dwell.domain.evidence.dto.response;

import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.domain.evidence.enums.Progression;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.common.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "EvidenceDetailResponse: 증거 상세 + 분석 결과 응답 DTO")
public class EvidenceDetailResponse {

    @Schema(description = "증거 ID", example = "6")
    private Long evidenceId;

    @Schema(description = "증거 번호", example = "EV-006")
    private String evidenceCode;

    @Schema(description = "AI가 정한 문제 이름 (없으면 null)", example = "Ceiling leak", nullable = true)
    private String title;

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "미디어 타입 (섞여 있으면 MULTI)", example = "MULTI")
    private MediaType mediaType;

    @Schema(description = "파일 구성 요약", example = "1 photo · 1 audio")
    private String mediaSummary;

    @Schema(description = "증거 단계", example = "INITIAL")
    private EvidencePhase phase;

    @Schema(description = "파일 목록")
    private List<FileItem> files;

    @Schema(description = "공간", example = "BATHROOM")
    private Location location;

    @Schema(description = "공간 직접 입력값 (location이 OTHER일 때)", nullable = true)
    private String locationDetail;

    @Schema(description = "AI가 추정한 부위 (분석 전이면 null)", example = "ceiling", nullable = true)
    private String subLocation;

    @Schema(description = "화면 표시 위치", example = "Bathroom · ceiling")
    private String locationLabel;

    @Schema(description = "사용자 설명", nullable = true)
    private String description;

    @Schema(description = "등록 시각", example = "2026-09-29T22:42:00")
    private LocalDateTime createdAt;

    @Schema(description = "AI 분석 상태 (PENDING -> DONE | FAILED)", example = "DONE")
    private AnalysisStatus analysisStatus;

    @Schema(description = "개별 판정 (분석 전이면 null)", nullable = true)
    private Analysis analysis;

    @Schema(description = "분석완료 화면 체크 3줄")
    private Integrity integrity;

    @Getter
    @Builder
    @Schema(title = "증거 파일")
    public static class FileItem {

        @Schema(description = "파일 공개 ID", example = "f_8a1c")
        private String fileId;

        @Schema(description = "미디어 타입", example = "PHOTO")
        private MediaType mediaType;

        @Schema(description = "원본 URL (presigned)")
        private String url;

        @Schema(description = "원본 SHA-256", example = "9f2b...e1")
        private String sha256;

        @Schema(description = "촬영 시각 (EXIF, 없으면 null)", nullable = true)
        private LocalDateTime capturedAt;

        @Schema(description = "영상·음성 길이(초), 없으면 null", nullable = true)
        private Integer durationSec;
    }

    @Getter
    @Builder
    @Schema(title = "개별 판정")
    public static class Analysis {

        @Schema(description = "이슈 유형", example = "WATER_LEAK")
        private IssueType issueType;

        @Schema(description = "심각도 (판단 불가면 null)", example = "HIGH", nullable = true)
        private Severity severity;

        @Schema(description = "시각적 근거")
        private List<LocalizedText> reasons;

        @Schema(description = "같은 위치 2시점 이상일 때만 SPREADING | STABLE | IMPROVING, 아니면 null", nullable = true)
        private Progression progression;

        @Schema(description = "진행 판단 근거", nullable = true)
        private LocalizedText progressionReason;
    }

    @Getter
    @Builder
    @Schema(title = "무결성 체크")
    public static class Integrity {

        @Schema(description = "Original file saved", example = "true")
        private boolean originalSaved;

        @Schema(description = "AI assessment attached", example = "true")
        private boolean aiAssessmentAttached;

        @Schema(description = "Protected from tampering (모든 파일에 SHA-256 기록)", example = "true")
        private boolean tamperProtected;
    }
}
