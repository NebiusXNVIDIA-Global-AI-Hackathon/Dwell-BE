package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    // 타임라인 순서 (같은 시각이면 기록 순)
    List<CaseEvent> findAllByCaseEntityOrderByOccurredAtAscIdAsc(Case caseEntity);

    // 가장 최근 이벤트 (새 이벤트 발생 시각 검증용)
    Optional<CaseEvent> findTopByCaseEntityOrderByOccurredAtDescIdDesc(Case caseEntity);
}
