package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseChecklistItemRepository extends JpaRepository<CaseChecklistItem, Long> {

    List<CaseChecklistItem> findAllByCaseEntityOrderBySortOrderAscIdAsc(Case caseEntity);
}
