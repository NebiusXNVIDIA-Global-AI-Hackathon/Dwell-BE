package com.dwell.domain.evidence.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Case caseEntity);
}
