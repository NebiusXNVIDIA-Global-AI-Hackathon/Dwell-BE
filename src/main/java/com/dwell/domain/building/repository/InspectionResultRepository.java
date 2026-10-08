package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.InspectionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionResultRepository extends JpaRepository<InspectionResult, Long> {
}
