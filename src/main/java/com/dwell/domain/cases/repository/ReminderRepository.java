package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}
