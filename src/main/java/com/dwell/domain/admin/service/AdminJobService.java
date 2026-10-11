package com.dwell.domain.admin.service;

import com.dwell.domain.admin.dto.response.JobRunResponse;
import com.dwell.domain.admin.exception.AdminErrorCode;
import com.dwell.domain.cases.service.CaseSlaJob;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
배치 수동 실행 (명세 jobName: sla-check | hpd-sync | building-pattern | repair-reminder)
지금은 sla-check만 지원, 나머지는 400 A001
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminJobService {

    private static final String SLA_CHECK = "sla-check";
    private static final DateTimeFormatter RUN_ID_FORMAT = DateTimeFormatter.ofPattern("'job_'yyyyMMdd_HHmmss");

    private final CaseSlaJob caseSlaJob;

    public JobRunResponse runJob(String jobName) {

        log.info("[AdminJobService] 배치 수동 실행 - 시작: jobName={}", jobName);

        if (!SLA_CHECK.equals(jobName)) {
            log.warn("[AdminJobService] 배치 수동 실행 실패 - 지원하지 않는 Job jobName: {}", jobName);
            throw new CustomException(AdminErrorCode.JOB_NOT_SUPPORTED);
        }

        CaseSlaJob.Result result = caseSlaJob.run();

        log.info("[AdminJobService] 배치 수동 실행 - 종료: jobName={}, transitioned={}", jobName, result.transitioned());

        // DTO 반환
        return JobRunResponse.builder()
                .runId(LocalDateTime.now().format(RUN_ID_FORMAT))
                .jobName(jobName)
                .checked(result.checked())
                .transitioned(result.transitioned())
                .build();
    }
}
