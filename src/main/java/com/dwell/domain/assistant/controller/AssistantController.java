package com.dwell.domain.assistant.controller;

import com.dwell.domain.assistant.dto.request.ChatRoomCreateRequest;
import com.dwell.domain.assistant.dto.response.ChatRoomCreateResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.service.AssistantService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 채팅방 생성 컨트롤러
    @Operation(summary = "채팅방 생성",
            description = "caseId가 없으면 일반 채팅방, 있으면 케이스 채팅방을 생성합니다. 케이스 채팅방이 이미 있으면 기존 채팅방을 반환합니다(200). 일반 채팅을 만드려면 caseId에 null 값")
    @PostMapping("/conversations")
    public ResponseEntity<BaseResponse<ChatRoomCreateResponse>> createChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) ChatRoomCreateRequest request) {

        // 채팅방 생성 서비스 호출
        ChatRoomCreateResponse response = assistantService.createChatRoom(userDetails.getUser().getId(), request);

        // 새로 만들었으면 201, 기존 채팅방이면 200
        if (response.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Chat room created.", response));
        }
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 채팅방 삭제 컨트롤러
    @Operation(summary = "채팅방 삭제",
            description = "채팅방과 메시지를 삭제합니다. 케이스 채팅방을 삭제해도 케이스는 유지됩니다.")
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId) {

        // 채팅방 삭제 서비스 호출
        assistantService.deleteChatRoom(userDetails.getUser().getId(), conversationId);

        // 응답 변환 (204 No Content)
        return ResponseEntity.noContent().build();
    }
}
