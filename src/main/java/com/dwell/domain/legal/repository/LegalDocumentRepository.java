package com.dwell.domain.legal.repository;

import com.dwell.domain.legal.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    Optional<LegalDocument> findByCode(String code);
}
