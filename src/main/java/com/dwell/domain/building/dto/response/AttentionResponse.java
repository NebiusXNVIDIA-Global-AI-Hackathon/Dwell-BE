package com.dwell.domain.building.dto.response;

import com.dwell.global.common.LocalizedText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "AttentionResponse DTO", description = "Needs Your Attention 카드 (내 건물에서 신고가 가장 많은 시그널)")
public class AttentionResponse {

    @Schema(description = "카드 문구 (native는 모국어 번역, 아직 없으면 null)")
    private LocalizedText text;

    @Schema(description = "시그널 ID (Check my unit → GET /places/me/inspection?signalId=)", example = "13")
    private Long signalId;
}
