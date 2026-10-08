package com.dwell.domain.evidence.repository;

import com.dwell.domain.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
}
