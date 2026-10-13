package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.BuildingSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingSignalRepository extends JpaRepository<BuildingSignal, Long> {

    // 건물 시그널 (신고 많은 순 -> 최근 계산 순)
    List<BuildingSignal> findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(Long buildingId);
}
