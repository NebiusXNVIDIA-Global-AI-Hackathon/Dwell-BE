package com.dwell.domain.building.repository;

import com.dwell.domain.building.entity.InspectionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionResultRepository extends JpaRepository<InspectionResult, Long> {

    // 사용자의 Check My Unit 결과 삭제 (회원 탈퇴 시)
    void deleteAllByUserId(Long userId);
}
