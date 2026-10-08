package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.AssistantAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantActionRepository extends JpaRepository<AssistantAction, Long> {
}
