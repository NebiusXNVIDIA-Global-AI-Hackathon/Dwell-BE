package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.SafetyAlert;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "케이스 입력 옵션 응답 DTO", description = "New Case 선택지, 안전 경고 문구, 증거 촬영 가이드 (고정 데이터라 클라에서 캐시해도 됨)")
public class CaseOptionsResponse {

    @Schema(description = "Step 1·2 이슈 카테고리와 세부 이슈")
    private List<IssueCategoryOption> issueCategories;

    @Schema(description = "Step 3·4 공간과 공간별 부위 (Entire Unit은 부위가 비어 있어 Step 4 건너뜀)")
    private List<LocationOption> locations;

    @Schema(description = "안전 경고 모달 문구 (키: ELECTRICAL, GAS, FLOODING)")
    private Map<SafetyAlert, SafetyAlertOption> safetyAlerts;

    @Schema(description = "증거 촬영 가이드 (Before You Go)")
    private EvidenceGuide evidenceGuide;

    @Getter
    @Builder
    @Schema(title = "이슈 카테고리 선택지")
    public static class IssueCategoryOption {

        @Schema(description = "카테고리 코드", example = "ELECTRICITY")
        private IssueCategory code;

        @Schema(description = "표시 문구", example = "Electricity")
        private String label;

        @Schema(description = "카테고리를 고르면 바로 띄우는 경고 (없으면 null)", example = "ELECTRICAL")
        private SafetyAlert safetyAlert;

        @Schema(description = "세부 이슈 (마지막은 직접 입력 OTHER)")
        private List<IssueDetailOption> details;
    }

    @Getter
    @Builder
    @Schema(title = "세부 이슈 선택지")
    public static class IssueDetailOption {

        @Schema(description = "세부 이슈 코드 (케이스 생성 요청의 issueDetail)", example = "FLOODING")
        private String code;

        @Schema(description = "표시 문구", example = "Flooding")
        private String label;

        @Schema(description = "고르면 띄우는 경고 (없으면 null)", example = "FLOODING")
        private SafetyAlert safetyAlert;
    }

    @Getter
    @Builder
    @Schema(title = "공간 선택지")
    public static class LocationOption {

        @Schema(description = "공간 코드", example = "BATHROOM")
        private Location code;

        @Schema(description = "표시 문구", example = "Bathroom")
        private String label;

        @Schema(description = "이 공간에서 고를 수 있는 부위 (와프 순서)")
        private List<AffectedAreaOption> affectedAreas;
    }

    @Getter
    @Builder
    @Schema(title = "부위 선택지")
    public static class AffectedAreaOption {

        @Schema(description = "부위 코드", example = "SHOWER")
        private AffectedArea code;

        @Schema(description = "표시 문구", example = "Shower / Tub")
        private String label;
    }

    @Getter
    @Builder
    @Schema(title = "안전 경고 모달 문구")
    public static class SafetyAlertOption {

        @Schema(description = "제목", example = "Electrical Safety Alert")
        private String title;

        @Schema(description = "굵은 첫 줄", example = "This may be an electrical hazard")
        private String headline;

        @Schema(description = "본문")
        private String body;
    }

    @Getter
    @Builder
    @Schema(title = "증거 촬영 가이드")
    public static class EvidenceGuide {

        @Schema(description = "제목", example = "Let's capture the right evidence")
        private String title;

        @Schema(description = "촬영 항목 (4장)")
        private List<EvidenceShot> shots;
    }

    @Getter
    @Builder
    @Schema(title = "촬영 항목")
    public static class EvidenceShot {

        @Schema(description = "항목 키", example = "OVERALL")
        private String key;

        @Schema(description = "제목", example = "Overall View")
        private String title;

        @Schema(description = "설명", example = "Show the entire area")
        private String description;
    }
}
