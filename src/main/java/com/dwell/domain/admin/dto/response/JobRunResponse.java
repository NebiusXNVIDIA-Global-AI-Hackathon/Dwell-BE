package com.dwell.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "JobRunResponse: 배치 수동 실행 응답 DTO")
public class JobRunResponse {

    @Schema(description = "실행 ID", example = "job_20261011_143000")
    private String runId;

    @Schema(description = "실행한 Job 이름", example = "sla-check")
    private String jobName;

    @Schema(description = "검사한 케이스 수", example = "5")
    private int checked;

    @Schema(description = "stage가 바뀐 케이스 수", example = "2")
    private int transitioned;
}
