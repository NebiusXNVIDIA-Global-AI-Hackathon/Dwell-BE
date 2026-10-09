package com.dwell.domain.building.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.enums.UsState;
import com.dwell.domain.building.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BuildingService {

    private final BuildingRepository buildingRepository;

    // BBL로 건물 조회, 없으면 새로 생성
    @Transactional
    public Building findOrCreate(String bbl, String street, String city, UsState state, String zip) {

        // 이미 등록된 건물인지 조회
        Optional<Building> existingBuilding = buildingRepository.findByBbl(bbl);

        // 이미 등록된 건물이면 그대로 사용
        if (existingBuilding.isPresent()) {
            return existingBuilding.get();
        }

        // 건물 객체 생성
        Building building =
                Building.builder()
                        .bbl(bbl)
                        .street(street)
                        .city(city)
                        .state(state)
                        .zip(zip)
                        .build();

        // DB 저장
        Building savedBuilding = buildingRepository.save(building);

        // 로그 출력
        log.info("[BuildingService] Building created: bbl={}", bbl);

        return savedBuilding;
    }
}
