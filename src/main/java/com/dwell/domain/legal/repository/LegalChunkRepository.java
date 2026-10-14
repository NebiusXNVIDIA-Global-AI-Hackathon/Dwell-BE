package com.dwell.domain.legal.repository;

import com.dwell.domain.legal.entity.LegalChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegalChunkRepository extends JpaRepository<LegalChunk, Long> {

    Optional<LegalChunk> findFirstByDocumentId(Long documentId);
}
