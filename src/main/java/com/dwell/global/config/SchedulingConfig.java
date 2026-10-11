package com.dwell.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 주기 실행 Job(SLA 체크) 켜기. dwell.sla-check.enabled=false면 주기 실행만 꺼지고 관리자 수동 실행은 그대로 동작
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "dwell.sla-check.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
