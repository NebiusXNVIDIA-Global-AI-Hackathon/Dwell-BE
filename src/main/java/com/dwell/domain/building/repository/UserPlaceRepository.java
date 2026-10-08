package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.UserPlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlaceRepository extends JpaRepository<UserPlace, Long> {
}
