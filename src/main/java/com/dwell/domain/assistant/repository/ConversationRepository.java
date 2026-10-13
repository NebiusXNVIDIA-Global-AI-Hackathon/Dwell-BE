package com.dwell.domain.assistant.repository;

import com.dwell.domain.assistant.entity.Conversation;
import com.dwell.domain.cases.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Fetch the linked case and inspection together to avoid N+1 on the chat list
    @Query("select c from Conversation c left join fetch c.caseEntity left join fetch c.inspection where c.user.id = :userId")
    List<Conversation> findAllWithCaseByUserId(@Param("userId") Long userId);

    // 케이스 채팅방 (케이스당 1개)
    Optional<Conversation> findByCaseEntityId(Long caseId);

    // 내 집 확인 채팅방 (검사 결과당 1개)
    Optional<Conversation> findByInspectionId(Long inspectionId);

    // Bring in a case 후보: 내 케이스 중 아직 채팅방이 없고 Resolved·삭제가 아닌 것 (최신 생성 순)
    @Query("select c from Case c where c.user.id = :userId and c.deletedAt is null "
            + "and c.stage <> com.dwell.domain.cases.enums.CaseStage.RESOLVED "
            + "and not exists (select v from Conversation v where v.caseEntity = c) "
            + "order by c.createdAt desc, c.id desc")
    List<Case> findCaseCandidates(@Param("userId") Long userId);

    // 케이스별 증거 사진 storage key ([caseId, storageKey], 증거 등록 순 -> 파일 순, 첫 번째를 썸네일로 사용)
    @Query("select e.caseEntity.id, f.storageKey from File f join f.evidence e "
            + "where e.caseEntity.id in :caseIds and e.deletedAt is null "
            + "and f.mediaType = com.dwell.global.common.enums.MediaType.PHOTO "
            + "order by e.createdAt, e.id, f.id")
    List<Object[]> findEvidencePhotoKeys(@Param("caseIds") Collection<Long> caseIds);
}
