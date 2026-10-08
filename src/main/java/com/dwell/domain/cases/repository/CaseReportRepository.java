package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.CaseReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseReportRepository extends JpaRepository<CaseReport, Long> {
}
