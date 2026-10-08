package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<Case, Long> {
}
