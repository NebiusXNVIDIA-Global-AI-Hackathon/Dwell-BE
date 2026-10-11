package com.dwell.domain.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*
SLA 체크 Job (명세 배치 이름 sla-check)
주기 실행(dwell.sla-check.interval, 기본 5분) + 관리자 수동 실행에서 같은 메소드 사용
케이스마다 별도 트랜잭션 → 한 건이 실패해도 나머지는 계속 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaseSlaJob {

    private final CaseSlaService caseSlaService;

    // 주기 실행 (dwell.sla-check.enabled=false면 SchedulingConfig가 꺼져 실행 안 됨)
    @Scheduled(fixedDelayString = "${dwell.sla-check.interval:PT5M}", initialDelayString = "${dwell.sla-check.initial-delay:PT1M}")
    public void scheduledRun() {
        run();
    }

    // 전체 대상 케이스 SLA 체크 - 결과 = 검사 수 · 전이 수
    public Result run() {

        log.info("[CaseSlaJob] SLA 체크 - 시작");

        LocalDateTime now = LocalDateTime.now();
        List<Long> caseIds = caseSlaService.findTargetCaseIds();

        int transitioned = 0;
        for (Long caseId : caseIds) {
            try {
                if (caseSlaService.checkCase(caseId, now)) {
                    transitioned++;
                }
            } catch (RuntimeException e) {
                log.error("[CaseSlaJob] SLA 체크 실패 - caseId: {}", caseId, e);
            }
        }

        log.info("[CaseSlaJob] SLA 체크 - 종료: checked={}, transitioned={}", caseIds.size(), transitioned);

        return new Result(caseIds.size(), transitioned);
    }

    public record Result(int checked, int transitioned) {
    }
}
