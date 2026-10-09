package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Fetch the linked case together to avoid N+1 on the chat list
    @Query("select c from Conversation c left join fetch c.caseEntity where c.user.id = :userId")
    List<Conversation> findAllWithCaseByUserId(@Param("userId") Long userId);
}
