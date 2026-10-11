package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.CaseStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, Long> {

    // 삭제되지 않은 케이스 + 주소용 건물, 소유자 확인용 유저 함께 조회
    @EntityGraph(attributePaths = {"user", "building"})
    Optional<Case> findByIdAndDeletedAtIsNull(Long id);

    // SLA 체크 대상 (기한이 걸린 stage의 삭제되지 않은 케이스)
    List<Case> findAllByStageInAndDeletedAtIsNull(Collection<CaseStage> stages);
}
