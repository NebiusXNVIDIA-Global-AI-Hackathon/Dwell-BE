package com.dwell.global.ai;

import com.dwell.domain.analysis.dto.VisionObservation;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiInspectorClientTest {

    private final GeminiInspectorClient client = new GeminiInspectorClient(JsonMapper.builder().build(), "", 60);

    @Test
    @DisplayName("튜닝 모델 응답(JSON) 파싱")
    void parse() {
        VisionObservation observation = client.parse("""
                {"defect_type": "water_stain_or_leak", "severity": "Medium", "severity_reason": "Large dry ring stain.",
                 "location": "ceiling", "visible_signs": ["brown ring stain"], "active_water_or_dampness": "no", "extent": "medium"}""");

        assertThat(observation.defectType()).isEqualTo("water_stain_or_leak");
        assertThat(observation.severity()).isEqualTo(AssessedSeverity.MEDIUM);
        assertThat(observation.visibleSigns()).containsExactly("brown ring stain");
    }

    @Test
    @DisplayName("모르는 유형·등급, JSON 아님, 근거 없음은 null (재시도 대상)")
    void parseInvalid() {
        assertThat(client.parse("{\"defect_type\":\"fire\",\"severity\":\"High\",\"severity_reason\":\"x\"}")).isNull();
        assertThat(client.parse("{\"defect_type\":\"mold\",\"severity\":\"Severe\",\"severity_reason\":\"x\"}")).isNull();
        assertThat(client.parse("{\"defect_type\":\"mold\",\"severity\":\"Low\"}")).isNull();
        assertThat(client.parse("not json")).isNull();
    }

    @Test
    @DisplayName("엔드포인트 미설정이면 호출 없이 Inconclusive")
    void notConfigured() {
        assertThat(client.isConfigured()).isFalse();
        assertThat(client.inspect(new byte[]{1}, "image/jpeg", false).severity()).isEqualTo(AssessedSeverity.INCONCLUSIVE);
    }
}
