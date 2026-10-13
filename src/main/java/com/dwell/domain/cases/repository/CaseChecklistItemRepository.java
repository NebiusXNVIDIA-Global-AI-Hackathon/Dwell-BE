package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseChecklistItemRepository extends JpaRepository<CaseChecklistItem, Long> {

    List<CaseChecklistItem> findAllByCaseEntityOrderBySortOrderAscIdAsc(Case caseEntity);

    // 케이스 안에서 항목 조회 (다른 케이스의 항목이면 비어 있음)
    Optional<CaseChecklistItem> findByIdAndCaseEntity(Long id, Case caseEntity);

    // 정렬 값이 가장 큰 항목 (새 항목을 맨 아래에 붙일 때 사용)
    Optional<CaseChecklistItem> findTopByCaseEntityOrderBySortOrderDesc(Case caseEntity);
}
