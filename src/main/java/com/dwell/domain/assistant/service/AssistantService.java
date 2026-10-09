package com.dwell.domain.assistant.service;

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
}
