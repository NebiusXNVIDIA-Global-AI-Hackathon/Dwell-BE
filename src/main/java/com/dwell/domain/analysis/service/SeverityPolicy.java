package com.dwell.domain.analysis.service;

import com.dwell.domain.analysis.dto.FinalAssessment;
import com.dwell.domain.analysis.dto.PolicyResult;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.dwell.global.common.enums.IssueCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
심각도 규칙 (LLM에 맡기지 않고 서버가 강제)
- 올리기: 제한 없음 (안전 쪽으로 틀리는 게 나음)
- 내리기: 사진 판정보다 최대 한 단계, 이유가 없으면 사진 판정 유지
- 사진이 Inconclusive: 위험 신호로 High가 된 경우만 인정, 아니면 Inconclusive + 재촬영 요청
- 난방·온수: 사진 판정 없음. 난방철(10/1~5/31) 난방 중단·부족, 온수 중단은 최소 High
- SLA 제안: High 24h / Medium 72h / Low 7일 (Inconclusive는 없음)
 */
@Component
public class SeverityPolicy {

    private static final MonthDay HEAT_SEASON_START = MonthDay.of(10, 1);
    private static final MonthDay HEAT_SEASON_END = MonthDay.of(5, 31);
    private static final Set<String> HEAT_OUTAGES = Set.of("NO_HEAT", "NOT_ENOUGH_HEAT");

    public PolicyResult apply(AssessedSeverity visionSeverity, FinalAssessment model,
                              IssueCategory issueCategory, String issueDetail, LocalDate today) {
        List<String> corrections = new ArrayList<>();
        AssessedSeverity proposed = model == null ? null : model.finalSeverity();

        // 난방·온수: 사진으로 판별할 수 없어 문제 종류로 최소 등급을 정함
        if (issueCategory == IssueCategory.HEAT_HOT_WATER) {
            AssessedSeverity result = proposed == null || proposed == AssessedSeverity.INCONCLUSIVE ? AssessedSeverity.MEDIUM : proposed;
            AssessedSeverity floor = heatFloor(issueDetail, today);
            if (floor != null && result.rank() < floor.rank()) {
                corrections.add("Raised to " + floor + " for " + issueDetail + (HEAT_OUTAGES.contains(issueDetail) ? " in heat season" : ""));
                result = floor;
            }
            return new PolicyResult(result, null, false, false, slaHours(result), corrections);
        }

        AssessedSeverity vision = visionSeverity == null ? AssessedSeverity.INCONCLUSIVE : visionSeverity;
        if (proposed == null) {
            corrections.add("No valid model result, kept the photo severity");
            proposed = vision;
        }

        // 사진으로 판단 불가: 설명의 위험 신호로 High가 된 경우만 인정
        if (vision == AssessedSeverity.INCONCLUSIVE) {
            AssessedSeverity result = proposed == AssessedSeverity.HIGH ? AssessedSeverity.HIGH : AssessedSeverity.INCONCLUSIVE;
            if (result != proposed) {
                corrections.add("Photo was inconclusive, " + proposed + " is not allowed without a danger signal");
            }
            return new PolicyResult(result, vision, result != vision, true, slaHours(result), corrections);
        }

        AssessedSeverity result = proposed;
        if (result == AssessedSeverity.INCONCLUSIVE) {
            corrections.add("Model returned INCONCLUSIVE for a clear photo, kept the photo severity");
            result = vision;
        } else if (result.rank() < vision.rank()) {
            String reason = model == null ? null : model.adjustmentReason();
            if (reason == null || reason.isBlank()) {
                corrections.add("Lowering without a reason is not allowed, kept the photo severity");
                result = vision;
            } else if (vision.rank() - result.rank() > 1) {
                corrections.add("Lowered by more than one level, limited to " + vision.oneLower());
                result = vision.oneLower();
            }
        }
        return new PolicyResult(result, vision, result != vision, false, slaHours(result), corrections);
    }

    // ========== 헬퍼 함수 ==========

    private static AssessedSeverity heatFloor(String issueDetail, LocalDate today) {
        if ("NO_HOT_WATER".equals(issueDetail)) {
            return AssessedSeverity.HIGH;
        }
        if (HEAT_OUTAGES.contains(issueDetail) && isHeatSeason(today)) {
            return AssessedSeverity.HIGH;
        }
        return null;
    }

    static boolean isHeatSeason(LocalDate today) {
        MonthDay day = MonthDay.from(today);
        return !day.isBefore(HEAT_SEASON_START) || !day.isAfter(HEAT_SEASON_END);
    }

    private static Integer slaHours(AssessedSeverity severity) {
        return switch (severity) {
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
            case INCONCLUSIVE -> null;
        };
    }
}
