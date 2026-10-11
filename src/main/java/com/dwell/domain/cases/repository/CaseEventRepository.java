package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    // 타임라인 순서 (같은 시각이면 기록 순)
    List<CaseEvent> findAllByCaseEntityOrderByOccurredAtAscIdAsc(Case caseEntity);
}
