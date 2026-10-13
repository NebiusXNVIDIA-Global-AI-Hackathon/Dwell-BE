package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.AssistantAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssistantActionRepository extends JpaRepository<AssistantAction, Long> {

    // 메시지들에 붙은 액션 (대화 조회용)
    List<AssistantAction> findAllByMessageIdIn(Collection<Long> messageIds);

    // 이 채팅방의 액션 (다른 채팅방 액션이면 없음)
    Optional<AssistantAction> findByIdAndMessageConversationId(Long id, Long conversationId);
}
