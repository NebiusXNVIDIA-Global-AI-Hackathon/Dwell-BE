package com.dwell.domain.analysis.service;

import com.dwell.domain.analysis.dto.FinalAssessment;
import com.dwell.domain.analysis.dto.PolicyResult;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.dwell.global.common.enums.IssueCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.dwell.domain.analysis.enums.AssessedSeverity.HIGH;
import static com.dwell.domain.analysis.enums.AssessedSeverity.INCONCLUSIVE;
import static com.dwell.domain.analysis.enums.AssessedSeverity.LOW;
import static com.dwell.domain.analysis.enums.AssessedSeverity.MEDIUM;
import static org.assertj.core.api.Assertions.assertThat;

class SeverityPolicyTest {

    private static final LocalDate WINTER = LocalDate.of(2026, 12, 1);
    private static final LocalDate SUMMER = LocalDate.of(2026, 7, 1);

    private final SeverityPolicy policy = new SeverityPolicy();

    @Test
    @DisplayName("데모: 사진 Medium + '물이 떨어져요' 설명으로 High 상향 -> 그대로 인정, SLA 24h")
    void raiseByDescription() {
        PolicyResult result = leak(MEDIUM, model(HIGH, "User reports active dripping."));

        assertThat(result.finalSeverity()).isEqualTo(HIGH);
        assertThat(result.adjusted()).isTrue();
        assertThat(result.proposedSlaHours()).isEqualTo(24);
        assertThat(result.corrections()).isEmpty();
    }

    @Test
    @DisplayName("하향은 한 단계까지: High 사진을 Low로 내리면 Medium으로 제한")
    void lowerLimitedToOneLevel() {
        PolicyResult result = leak(HIGH, model(LOW, "Looks old."));

        assertThat(result.finalSeverity()).isEqualTo(MEDIUM);
        assertThat(result.proposedSlaHours()).isEqualTo(72);
        assertThat(result.corrections()).hasSize(1);
    }

    @Test
    @DisplayName("이유 없는 하향은 사진 판정 유지")
    void lowerWithoutReason() {
        assertThat(leak(MEDIUM, model(LOW, null)).finalSeverity()).isEqualTo(MEDIUM);
    }

    @Test
    @DisplayName("이유 있는 한 단계 하향은 인정, SLA 7일")
    void lowerOneLevelWithReason() {
        PolicyResult result = leak(MEDIUM, model(LOW, "Tenant says it is an old dry stain."));

        assertThat(result.finalSeverity()).isEqualTo(LOW);
        assertThat(result.proposedSlaHours()).isEqualTo(168);
    }

    @Test
    @DisplayName("사진 Inconclusive + 위험 신호 없음 -> Inconclusive, 재촬영 요청, SLA 없음")
    void inconclusivePhoto() {
        PolicyResult result = leak(INCONCLUSIVE, model(MEDIUM, "guess"));

        assertThat(result.finalSeverity()).isEqualTo(INCONCLUSIVE);
        assertThat(result.needsRetake()).isTrue();
        assertThat(result.proposedSlaHours()).isNull();
    }

    @Test
    @DisplayName("사진 Inconclusive + 위험 신호로 High -> High, 재촬영도 요청")
    void inconclusivePhotoWithDanger() {
        PolicyResult result = leak(INCONCLUSIVE, model(HIGH, "Ceiling is sagging."));

        assertThat(result.finalSeverity()).isEqualTo(HIGH);
        assertThat(result.needsRetake()).isTrue();
    }

    @Test
    @DisplayName("모델 결과가 없으면 사진 판정 그대로")
    void noModel() {
        PolicyResult result = leak(MEDIUM, null);

        assertThat(result.finalSeverity()).isEqualTo(MEDIUM);
        assertThat(result.adjusted()).isFalse();
    }

    @Test
    @DisplayName("선명한 사진인데 모델이 Inconclusive면 사진 판정 유지")
    void modelInconclusiveOnClearPhoto() {
        assertThat(leak(LOW, model(INCONCLUSIVE, null)).finalSeverity()).isEqualTo(LOW);
    }

    @Test
    @DisplayName("난방철 난방 중단은 최소 High (모델이 Medium이어도)")
    void noHeatInSeason() {
        PolicyResult result = policy.apply(null, model(MEDIUM, null), IssueCategory.HEAT_HOT_WATER, "NO_HEAT", WINTER);

        assertThat(result.finalSeverity()).isEqualTo(HIGH);
        assertThat(result.visionSeverity()).isNull();
        assertThat(result.needsRetake()).isFalse();
    }

    @Test
    @DisplayName("난방철이 아니면 난방 중단에 최소 등급 없음, 모델 없으면 Medium")
    void noHeatOutOfSeason() {
        assertThat(policy.apply(null, null, IssueCategory.HEAT_HOT_WATER, "NO_HEAT", SUMMER).finalSeverity()).isEqualTo(MEDIUM);
    }

    @Test
    @DisplayName("온수 중단은 계절과 관계없이 High")
    void noHotWater() {
        assertThat(policy.apply(null, model(LOW, "x"), IssueCategory.HEAT_HOT_WATER, "NO_HOT_WATER", SUMMER).finalSeverity())
                .isEqualTo(HIGH);
    }

    @Test
    @DisplayName("난방철: 10/1 ~ 5/31")
    void heatSeason() {
        assertThat(SeverityPolicy.isHeatSeason(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(SeverityPolicy.isHeatSeason(LocalDate.of(2027, 5, 31))).isTrue();
        assertThat(SeverityPolicy.isHeatSeason(LocalDate.of(2026, 6, 1))).isFalse();
        assertThat(SeverityPolicy.isHeatSeason(LocalDate.of(2026, 9, 30))).isFalse();
    }

    private PolicyResult leak(AssessedSeverity vision, FinalAssessment model) {
        return policy.apply(vision, model, IssueCategory.WATER_FLOODING, "WATER_LEAK", WINTER);
    }

    private static FinalAssessment model(AssessedSeverity severity, String reason) {
        return new FinalAssessment(severity, reason, "user_description", "Not_applicable", null, List.of());
    }
}
