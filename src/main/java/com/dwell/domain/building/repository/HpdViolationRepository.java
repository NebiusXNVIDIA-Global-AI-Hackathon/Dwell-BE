package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.HpdViolation;
import com.dwell.domain.building.enums.HpdRecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HpdViolationRepository extends JpaRepository<HpdViolation, Long> {

    // 건물 HPD 민원·위반 (기간 내 전체, 집계용)
    List<HpdViolation> findAllByBuildingIdAndReportedAtGreaterThanEqual(Long buildingId, LocalDate since);

    // 건물 HPD 민원 또는 위반 목록 (최신 순, HPD History 탭)
    Page<HpdViolation> findByBuildingIdAndRecordTypeAndReportedAtGreaterThanEqualOrderByReportedAtDescIdDesc(
            Long buildingId, HpdRecordType recordType, LocalDate since, Pageable pageable);
}
