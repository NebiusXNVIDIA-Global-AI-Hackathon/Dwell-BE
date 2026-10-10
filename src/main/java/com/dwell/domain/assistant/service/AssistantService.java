package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.request.ChatRoomCreateRequest;
import com.dwell.domain.assistant.dto.response.ChatRoomCreateResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;

public interface AssistantService {

    /**
     * [채팅 목록 조회 서비스]
     * @author 김민호
     * @description: Case 채팅방과 Other 채팅방을 조회합니다.
     *
     * @request userId
     * @return ConversationListResponse(Case 채팅방, Other 채팅방)
     */
    ConversationListResponse getChatRooms(Long userId);

    /**
     * [채팅방 생성 서비스]
     * @author 김민호
     * @description: caseId가 없으면 Other 채팅방, 있으면 Case 채팅방을 생성합니다.
     *               Case 채팅방은 케이스당 1개라 이미 있으면 기존 채팅방을 반환합니다.
     *
     * @request userId, ChatRoomCreateRequest(caseId, intent)
     * @return ChatRoomCreateResponse(채팅방 정보, 케이스 요약, Deaver 인사 메시지)
     */
    ChatRoomCreateResponse createChatRoom(Long userId, ChatRoomCreateRequest request);

    /**
     * [채팅방 삭제 서비스]
     * @author 김민호
     * @description: 채팅방과 메시지를 삭제합니다. Case 채팅방을 삭제해도 케이스는 유지됩니다.
     *
     * @request userId, conversationId
     * @return void
     */
    void deleteChatRoom(Long userId, Long conversationId);
}
