package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
