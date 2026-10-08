package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
