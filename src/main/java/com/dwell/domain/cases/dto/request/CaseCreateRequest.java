package com.dwell.domain.cases.dto.request;

import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.Visibility;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(title = "케이스 생성 요청 DTO", description = "New Case 단계별 선택값, 증거 파일, 설명, 공개 설정")
public class CaseCreateRequest {

    @Schema(description = "1단계 이슈 카테고리", example = "WATER_FLOODING")
    @NotNull(message = "Issue category is a required field.")
    private IssueCategory issueCategory;

    @Schema(description = "2단계 세부 이슈 코드 (직접 입력이면 OTHER)", example = "WATER_LEAK")
    @NotBlank(message = "Issue detail is a required field.")
    @Size(max = 30, message = "Issue detail must be 30 characters or less.")
    @Pattern(regexp = "^[A-Z_]+$", message = "Issue detail must be an uppercase code.")
    private String issueDetail;

    @Schema(description = "세부 이슈가 OTHER일 때 직접 입력한 값", example = "Rainwater coming in")
    @Size(max = 50, message = "Issue detail (Other) must be 50 characters or less.")
    private String issueDetailOther;

    @Schema(description = "3단계 공간", example = "BATHROOM")
    @NotNull(message = "Location is a required field.")
    private Location location;

    @Schema(description = "공간이 OTHER일 때 직접 입력한 값", example = "Garage")
    @Size(max = 50, message = "Location (Other) must be 50 characters or less.")
    private String locationOther;

    @Schema(description = "4단계 부위 (공간이 ENTIRE_UNIT이면 비움)", example = "CEILING")
    private AffectedArea affectedArea;

    @Schema(description = "부위가 OTHER일 때 직접 입력한 값", example = "Behind the mirror")
    @Size(max = 50, message = "Affected area (Other) must be 50 characters or less.")
    private String affectedAreaOther;

    @Schema(description = "6단계 설명 (선택)", example = "Water is leaking from the ceiling above the shower.")
    @Size(max = 500, message = "Description must be 500 characters or less.")
    private String description;

    // EVIDENCE_REQUIRED 에러 코드로 응답하려고 @NotEmpty 대신 서비스에서 검사
    @Schema(description = "POST /files로 올린 파일 ID (1개 이상)", example = "[101, 102, 103]")
    private List<Long> fileIds;

    @Schema(description = "7단계 공개 설정 (PUBLIC: Building Signal에 익명 공유, PRIVATE: 나만 보기)", example = "PUBLIC")
    @NotNull(message = "Visibility is a required field.")
    private Visibility visibility;

    @Schema(description = "안전 경고에서 I'm Safe를 눌렀는지", example = "true")
    private boolean safetyAcknowledged;
}
