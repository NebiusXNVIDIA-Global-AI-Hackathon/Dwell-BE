package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {
}
