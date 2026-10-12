package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.UserPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPlaceRepository extends JpaRepository<UserPlace, Long> {

    // 사용자의 집 정보 조회 (케이스 생성 시 주소 스냅샷용)
    Optional<UserPlace> findByUserId(Long userId);
}
