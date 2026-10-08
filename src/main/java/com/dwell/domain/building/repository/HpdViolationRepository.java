package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.HpdViolation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HpdViolationRepository extends JpaRepository<HpdViolation, Long> {
}
