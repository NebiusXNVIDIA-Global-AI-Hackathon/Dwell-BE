package com.dwell.domain.legal.service;

import com.dwell.TestcontainersConfig;
import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.entity.LegalDocument;
import com.dwell.domain.legal.enums.Jurisdiction;
import com.dwell.domain.legal.repository.LegalChunkRepository;
import com.dwell.domain.legal.repository.LegalDocumentRepository;
import com.dwell.global.common.enums.IssueCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 PostgreSQL(Testcontainers)에서 V7 마이그레이션 + 시작 시 코퍼스 적재 확인
@SpringBootTest
@Import(TestcontainersConfig.class)
class LegalCorpusLoaderTest {

    @Autowired
    private LegalCorpusLoader legalCorpusLoader;
    @Autowired
    private LegalDocumentRepository legalDocumentRepository;
    @Autowired
    private LegalChunkRepository legalChunkRepository;
    @Autowired
    private LegalSourceService legalSourceService;

    @Test
    @DisplayName("시작 시 yaml 전체가 문서·청크로 적재되고, 다시 돌려도 중복이 없음")
    void loadsCorpusIdempotently() {
        int entries = LegalCorpusLoader.readEntries().size();
        assertThat(entries).isEqualTo(26);
        assertThat(legalDocumentRepository.count()).isEqualTo(entries);
        assertThat(legalChunkRepository.count()).isEqualTo(entries);

        legalCorpusLoader.run(new DefaultApplicationArguments());

        assertThat(legalDocumentRepository.count()).isEqualTo(entries);
        assertThat(legalChunkRepository.count()).isEqualTo(entries);
    }

    @Test
    @DisplayName("yaml 필드가 그대로 매핑됨 (관할은 id 접두어로)")
    void mapsFields() {
        LegalDocument heat = legalDocumentRepository.findByCode("NYC-HMC-27-2029").orElseThrow();
        assertThat(heat.getLabel()).isEqualTo("NYC Admin. Code § 27-2029");
        assertThat(heat.getIssueTypes()).containsExactly("heat");
        assertThat(heat.getJurisdiction()).isEqualTo(Jurisdiction.NYC);
        assertThat(heat.getSummary()).doesNotContain("\n");

        assertThat(legalDocumentRepository.findByCode("NYS-RPL-223-B").orElseThrow().getJurisdiction()).isEqualTo(Jurisdiction.NYS);
    }

    @Test
    @DisplayName("누수 케이스 채팅 근거: 누수 조항 포함, 난방 전용 조항 제외")
    void leakSources() {
        List<String> codes = legalSourceService.findSources(IssueCategory.WATER_FLOODING, LegalSourceService.USAGE_ASSISTANT)
                .stream().map(LegalSource::code).toList();
        assertThat(codes).contains("NYC-HMC-27-2026", "NYS-RPL-235-B").doesNotContain("NYC-HMC-27-2029");
    }
}
