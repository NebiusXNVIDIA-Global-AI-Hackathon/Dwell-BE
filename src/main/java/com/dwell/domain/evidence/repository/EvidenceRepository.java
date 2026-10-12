package com.dwell.domain.evidence.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Case caseEntity);

    // 사용자의 전체 증거 수 (증거 번호 생성용)
    long countByUserId(Long userId);
}
