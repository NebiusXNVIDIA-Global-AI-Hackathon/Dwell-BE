package com.dwell.domain.assistant.controller;

import com.dwell.domain.assistant.dto.request.ActionChooseRequest;
import com.dwell.domain.assistant.dto.request.ChatRoomCreateRequest;
import com.dwell.domain.assistant.dto.request.MessageSendRequest;
import com.dwell.domain.assistant.dto.response.ActionResultResponse;
import com.dwell.domain.assistant.dto.response.CaseCandidatesResponse;
import com.dwell.domain.assistant.dto.response.ChatRoomCreateResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.dto.response.ConversationMessagesResponse;
import com.dwell.domain.assistant.service.AssistantChatService;
import com.dwell.domain.assistant.service.AssistantService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Assistant", description = "AI 상담 API")
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantChatService assistantChatService;

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

    // 대화 조회 컨트롤러
    @Operation(summary = "대화 조회",
            description = "채팅방 메시지를 오래된 순으로 줍니다 (기본 30개, 최대 50개). hasMore가 true면 before=가장 오래된 messageId로 이전 메시지를 더 불러오세요.")
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<BaseResponse<ConversationMessagesResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "30") int size) {

        // 대화 조회 서비스 호출
        ConversationMessagesResponse response = assistantService.getMessages(userDetails.getUser().getId(), conversationId, before, size);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 메시지 전송 (SSE 스트리밍) 컨트롤러
    @Operation(summary = "메시지 전송 (SSE 스트리밍)",
            description = "메시지를 저장하고 Deaver(Nemotron) 답변을 text/event-stream으로 보냅니다. "
                    + "이벤트 순서: token({delta}) 여러 번 -> card -> action({actionId, type, choices}) -> quick_replies({items}) -> done({messageId}). "
                    + "실패하면 error({code, message}). 서버에 NEBIUS_API_KEY가 없으면 스트림을 열기 전에 503입니다.")
    @PostMapping(value = "/conversations/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId,
            @Valid @RequestBody MessageSendRequest request) {

        // 메시지 전송 서비스 호출 (답변은 스트림으로)
        return assistantChatService.sendMessage(userDetails.getUser().getId(), conversationId, request);
    }

    // 채팅 액션 실행 컨트롤러
    @Operation(summary = "채팅 액션 실행",
            description = "메시지 선택 버튼(choices[].key)을 실행합니다. CREATE_CASE+YES면 prefill, SEND_MESSAGE+YES면 보낼 본문(messageBody, "
                    + "Edit message로 고쳤으면 editedBody), APPLY_LANDLORD_REPLY+YES면 답장 저장·케이스 이벤트 기록 후 회신 초안 메시지(nextMessage)를 줍니다. "
                    + "이미 고른 액션은 409입니다.")
    @PostMapping("/conversations/{conversationId}/actions/{actionId}")
    public ResponseEntity<BaseResponse<ActionResultResponse>> chooseAction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId,
            @PathVariable Long actionId,
            @Valid @RequestBody ActionChooseRequest request) {

        // 액션 실행 서비스 호출
        ActionResultResponse response = assistantChatService.chooseAction(userDetails.getUser().getId(), conversationId, actionId, request);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // Bring in a case 후보 목록 컨트롤러
    @Operation(summary = "Bring in a case 후보 목록",
            description = "아직 채팅방이 없고 Resolved가 아닌 내 케이스를 상태별(LOGGED -> IN_PROGRESS)로 줍니다. 고른 케이스로 POST /conversations {caseId}를 호출하세요.")
    @GetMapping("/case-candidates")
    public ResponseEntity<BaseResponse<CaseCandidatesResponse>> getCaseCandidates(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 후보 목록 서비스 호출
        CaseCandidatesResponse response = assistantService.getCaseCandidates(userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
