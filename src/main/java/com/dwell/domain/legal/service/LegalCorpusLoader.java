package com.dwell.domain.legal.service;

import com.dwell.domain.legal.entity.LegalChunk;
import com.dwell.domain.legal.entity.LegalDocument;
import com.dwell.domain.legal.enums.Jurisdiction;
import com.dwell.domain.legal.enums.LegalTextType;
import com.dwell.domain.legal.repository.LegalChunkRepository;
import com.dwell.domain.legal.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/*
법률 코퍼스(resources/legal/legal_corpus.yaml) -> DB 동기화 (서버 시작 시 1회)
- yaml 항목 1개 = legal_document 1개 + legal_chunk 1개 (content = 원문 text)
- code(yaml id) 기준 upsert라 여러 번 돌려도 중복 없음, yaml에서 지운 항목은 DB에 남음
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegalCorpusLoader implements ApplicationRunner {

    static final String CORPUS_PATH = "legal/legal_corpus.yaml";

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalChunkRepository legalChunkRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> entries = readEntries();
        for (Map<String, Object> entry : entries) {
            upsert(entry);
        }
        log.info("[LegalCorpusLoader] 법률 코퍼스 동기화 완료: {}개", entries.size());
    }

    // yaml의 chunks 목록 읽기
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> readEntries() {
        try (InputStream in = new ClassPathResource(CORPUS_PATH).getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            return (List<Map<String, Object>>) root.get("chunks");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + CORPUS_PATH, e);
        }
    }

    // ========== 헬퍼 함수 ==========

    @SuppressWarnings("unchecked")
    private void upsert(Map<String, Object> entry) {
        String code = (String) entry.get("id");
        String label = (String) entry.get("citation");
        String title = (String) entry.get("title");
        String url = (String) entry.get("url");
        Jurisdiction jurisdiction = code.startsWith("NYS-") ? Jurisdiction.NYS : Jurisdiction.NYC;
        List<String> issueTypes = (List<String>) entry.get("issue_types");
        List<String> usedBy = (List<String>) entry.get("used_by");
        LegalTextType textType = LegalTextType.valueOf(((String) entry.get("text_type")).toUpperCase());
        String summary = oneLine((String) entry.get("summary"));
        String content = ((String) entry.get("text")).strip();

        LegalDocument document = legalDocumentRepository.findByCode(code).orElse(null);
        if (document == null) {
            document = legalDocumentRepository.save(LegalDocument.builder()
                    .code(code)
                    .label(label)
                    .title(title)
                    .url(url)
                    .jurisdiction(jurisdiction)
                    .issueTypes(issueTypes)
                    .usedBy(usedBy)
                    .textType(textType)
                    .summary(summary)
                    .build());
        } else {
            document.update(label, title, url, jurisdiction, issueTypes, usedBy, textType, summary);
        }

        LegalDocument saved = document;
        legalChunkRepository.findFirstByDocumentId(saved.getId()).ifPresentOrElse(
                chunk -> chunk.updateContent(content),
                () -> legalChunkRepository.save(LegalChunk.builder().document(saved).content(content).build()));
    }

    // yaml 블록 문자열의 줄바꿈·연속 공백을 한 칸으로
    private static String oneLine(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }
}
