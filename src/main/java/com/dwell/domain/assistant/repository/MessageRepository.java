package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.Message;
import com.dwell.domain.assistant.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Latest message of each conversation
    @Query("""
            select m from Message m
            where m.id in (
                select max(m2.id) from Message m2
                where m2.conversation.id in :conversationIds
                group by m2.conversation.id
            )
            """)
    List<Message> findLastMessages(@Param("conversationIds") Collection<Long> conversationIds);

    // First message of each conversation sent by the given role
    @Query("""
            select m from Message m
            where m.id in (
                select min(m2.id) from Message m2
                where m2.conversation.id in :conversationIds and m2.role = :role
                group by m2.conversation.id
            )
            """)
    List<Message> findFirstMessages(@Param("conversationIds") Collection<Long> conversationIds,
                                    @Param("role") MessageRole role);
}
