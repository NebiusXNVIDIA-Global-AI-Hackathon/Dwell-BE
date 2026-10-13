package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.request.ChatRoomCreateRequest;
import com.dwell.domain.assistant.dto.response.CaseCandidatesResponse;
import com.dwell.domain.assistant.dto.response.ChatRoomCreateResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.dto.response.ConversationMessagesResponse;

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

    /**
     * [대화 조회 서비스]
     * @author 김민호
     * @description: 채팅방 메시지를 오래된 순으로 조회합니다 (카드·법률 근거·빠른 답장·선택 버튼·첨부 포함).
     *               before(messageId)를 주면 그보다 이전 메시지를 size개 조회합니다.
     *
     * @request userId, conversationId, before, size
     * @return ConversationMessagesResponse(conversationId, type, case, messages, hasMore)
     */
    ConversationMessagesResponse getMessages(Long userId, Long conversationId, Long before, int size);

    /**
     * [Bring in a case 후보 목록 서비스]
     * @author 김민호
     * @description: 아직 채팅방이 없고 Resolved·삭제가 아닌 내 케이스를 상태별(LOGGED -> IN_PROGRESS)로 묶어 줍니다.
     *
     * @request userId
     * @return CaseCandidatesResponse(groups)
     */
    CaseCandidatesResponse getCaseCandidates(Long userId);
}
