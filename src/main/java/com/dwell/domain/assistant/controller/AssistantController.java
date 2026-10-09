package com.dwell.domain.assistant.controller;

import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.service.AssistantService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assistant", description = "AI 상담 API")
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    // 채팅 목록 조회 컨트롤러
    @Operation(summary = "채팅 목록 조회",
            description = "진행 중인 케이스 대화(Resolved 제외)와 일반 대화를 최근 메시지 순으로 조회합니다.")
    @GetMapping("/conversations")
    public ResponseEntity<BaseResponse<ConversationListResponse>> getChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 채팅방 목록 조회 서비스 호출
        ConversationListResponse response = assistantService.getChatRooms(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
