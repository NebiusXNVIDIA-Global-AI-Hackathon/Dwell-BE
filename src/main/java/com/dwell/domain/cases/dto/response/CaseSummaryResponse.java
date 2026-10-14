package com.dwell.domain.cases.dto.response;

import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.common.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "CaseSummaryResponse: 케이스 분석 (View Case Analysis) 응답 DTO")
public class CaseSummaryResponse {

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-006")
    private String caseNumber;

    @Schema(description = "화면 제목", example = "Water Leak in Bathroom · Ceiling")
    private String headline;

    @Schema(description = "주소 (건물 + 호수)", example = "123 Main St, Apt 4B")
    private String address;

    @Schema(description = "AI 분석 상태 (PENDING이면 아래 분석 값이 아직 없거나 이전 값)", example = "DONE")
    private AnalysisStatus analysisStatus;

    @Schema(description = "진행 판단")
    private Progress progress;

    @Schema(description = "Issue Details")
    private IssueDetails issueDetails;

    @Schema(description = "Why it's high severity (최대 3개, 분석 전이면 빈 배열)")
    private List<LocalizedText> severityReasons;

    @Schema(description = "사진이 판단하기 어려워 다시 찍어야 하는지", example = "false")
    private boolean needsRetake;

    @Schema(description = "제안 수리 기한(시간). HIGH 24 / MEDIUM 72 / LOW 168, 판단 불가면 null", example = "24", nullable = true)
    private Integer proposedSlaHours;

    @Schema(description = "건물 패턴 (없으면 null)", nullable = true)
    private BuildingSignal buildingSignal;

    @Schema(description = "타임라인 미리보기 (최근 2건, 오래된 순)")
    private List<CaseTimelineEventResponse> timelinePreview;

    @Schema(description = "Your Rights (Legal RAG, 출처 있는 항목만)")
    private List<Right> rights;

    @Schema(description = "증거 썸네일")
    private List<EvidenceThumbnail> evidence;

    @Schema(description = "분석 시각 (분석 전이면 null)", example = "2026-09-28T22:42:00", nullable = true)
    private LocalDateTime analysisUpdatedAt;

    @Schema(description = "사용자가 분석을 수정했는지", example = "false")
    private boolean editedByUser;

    @Getter
    @Builder
    @Schema(title = "진행 판단")
    public static class Progress {

        @Schema(description = "ON_TRACK | NEEDS_ATTENTION | ESCALATE", example = "ON_TRACK")
        private String verdict;

        @Schema(description = "판단 문구")
        private LocalizedText message;
    }

    @Getter
    @Builder
    @Schema(title = "Issue Details")
    public static class IssueDetails {

        @Schema(description = "이슈", example = "Water Leak")
        private String issueType;

        @Schema(description = "위치", example = "Bathroom · Ceiling")
        private String location;

        @Schema(description = "처음 발견한 날 (없으면 null)", example = "2026-09-28", nullable = true)
        private LocalDate firstNoticedAt;

        @Schema(description = "집주인에게 처음 알린 시각 (없으면 null)", example = "2026-09-28T10:00:00", nullable = true)
        private LocalDateTime reportedToLandlordAt;

        @Schema(description = "심각도 (분석 전·판단 불가면 null)", example = "HIGH", nullable = true)
        private Severity severity;
    }

    @Getter
    @Builder
    @Schema(title = "건물 패턴")
    public static class BuildingSignal {

        @Schema(description = "케이스 분석에 포함했는지 (Add to my case analysis)", example = "false")
        private boolean included;

        @Schema(description = "건물 패턴 문구")
        private LocalizedText text;

        @Schema(description = "건물 패턴 ID (See a building signals)", example = "12")
        private Long signalId;
    }

    @Getter
    @Builder
    @Schema(title = "Your Rights 항목")
    public static class Right {

        @Schema(description = "제목", example = "Duties of owner")
        private String title;

        @Schema(description = "요약")
        private String body;

        @Schema(description = "출처", example = "[\"NYC Admin. Code § 27-2005\"]")
        private List<String> citations;

        @Schema(description = "원문 링크")
        private String url;
    }

    @Getter
    @Builder
    @Schema(title = "증거 썸네일")
    public static class EvidenceThumbnail {

        @Schema(description = "증거 ID", example = "6")
        private Long evidenceId;

        @Schema(description = "미디어 타입", example = "PHOTO")
        private MediaType mediaType;

        @Schema(description = "첫 번째 파일 URL (presigned)")
        private String thumbnailUrl;

        @Schema(description = "영상·음성 길이(초), 없으면 null", example = "18", nullable = true)
        private Integer durationSec;

        @Schema(description = "등록 시각", example = "2026-09-28T20:12:00")
        private LocalDateTime createdAt;
    }
}
