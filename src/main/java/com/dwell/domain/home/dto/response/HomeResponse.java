package com.dwell.domain.home.dto.response;

import com.dwell.domain.building.dto.response.AttentionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "HomeResponse DTO", description = "홈 화면 데이터 (사용자, 진행 중 케이스 카드, Needs Your Attention)")
public class HomeResponse {

    @Schema(description = "상단 아바타 사용자 정보")
    private UserInfo user;

    @Schema(description = "진행 중 케이스 카드 (응답 기한 임박 → 최근 업데이트 순, 없으면 [])")
    private List<HomeCaseResponse> cases;

    @Schema(description = "Needs Your Attention 카드 (집 미등록이거나 시그널이 없으면 null)")
    private AttentionResponse attention;

    @Getter
    @Builder
    @Schema(title = "홈 사용자 정보")
    public static class UserInfo {

        @Schema(description = "닉네임 (아바타 표시용)", example = "seohu")
        private String nickname;
    }
}
