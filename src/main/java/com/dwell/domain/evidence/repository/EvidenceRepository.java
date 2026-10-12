package com.dwell.domain.evidence.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Case caseEntity);

    // 사용자의 가장 최근 증거 (증거 번호 생성용)
    Optional<Evidence> findTopByUserIdOrderByIdDesc(Long userId);
}
