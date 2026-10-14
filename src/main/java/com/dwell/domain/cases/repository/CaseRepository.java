package com.dwell.domain.cases.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.global.common.enums.IssueCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, Long> {

    // 삭제되지 않은 케이스 + 주소용 건물, 소유자 확인용 유저 함께 조회
    @EntityGraph(attributePaths = {"user", "building"})
    Optional<Case> findByIdAndDeletedAtIsNull(Long id);

    // SLA 체크 대상 (기한이 걸린 stage의 삭제되지 않은 케이스)
    List<Case> findAllByStageInAndDeletedAtIsNull(Collection<CaseStage> stages);

    // 사용자의 가장 최근 케이스 (케이스 번호 생성용)
    Optional<Case> findTopByUserIdOrderByIdDesc(Long userId);

    // 사용자의 진행 중(Resolved 아님) 케이스 이슈 카테고리 (커뮤니티 추천 글용)
    @Query("select distinct c.issueCategory from Case c where c.user.id = :userId and c.deletedAt is null "
            + "and c.stage <> com.dwell.domain.cases.enums.CaseStage.RESOLVED")
    List<IssueCategory> findActiveIssueCategoriesByUserId(Long userId);

    // 사용자의 진행 중 케이스 (Resolved·삭제 제외, 홈 카드용)
    List<Case> findAllByUserIdAndDeletedAtIsNullAndStageNot(Long userId, CaseStage stage);

    // 사용자의 케이스 삭제 (회원 탈퇴 시, 증거·타임라인 등은 DB의 ON DELETE CASCADE)
    void deleteAllByUserId(Long userId);
}
