package com.dwell.domain.file.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {

    // 증거 여러 건의 파일을 한 번에 (썸네일·개수 계산용)
    List<File> findAllByEvidenceInOrderByIdAsc(List<Evidence> evidences);

    // 케이스의 모든 증거 파일 (케이스 삭제 시 파일 정리용, 증거 엔티티는 불러오지 않음)
    List<File> findAllByEvidenceCaseEntity(Case caseEntity);

    // 저장 경로로 파일 조회 (사용자에게는 프로필 사진 경로만 있어서, 이전 사진을 지울 때 사용)
    Optional<File> findByStorageKey(String storageKey);
}
