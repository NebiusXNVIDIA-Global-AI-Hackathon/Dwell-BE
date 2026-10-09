package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    // 건물이 존재하는지 조회
    Optional<Building> findByBbl(String bbl);
}
