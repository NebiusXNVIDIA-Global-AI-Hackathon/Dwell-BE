package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.CaseChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseChecklistItemRepository extends JpaRepository<CaseChecklistItem, Long> {
}
