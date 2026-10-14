package com.dwell.domain.legal.service;

import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.entity.LegalDocument;
import com.dwell.domain.legal.enums.Jurisdiction;
import com.dwell.domain.legal.enums.LegalTextType;
import com.dwell.domain.legal.repository.LegalDocumentRepository;
import com.dwell.global.common.enums.IssueCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LegalSourceServiceTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @InjectMocks
    private LegalSourceService legalSourceService;

    @BeforeEach
    void setUp() {
        given(legalDocumentRepository.findAll(any(Sort.class))).willReturn(List.of(
                document("GENERAL", List.of("all"), List.of("notice", "assistant")),
                document("HEAT", List.of("heat"), List.of("assistant")),
                document("HOT-WATER", List.of("hot_water"), List.of("assistant")),
                document("LEAK", List.of("leak"), List.of("assistant")),
                document("MOLD-NOTICE-ONLY", List.of("mold"), List.of("notice"))));
    }

    @Test
    @DisplayName("난방 케이스: 난방·온수 조항 + 공통 조항만")
    void heatCase() {
        assertThat(codes(IssueCategory.HEAT_HOT_WATER, "assistant")).containsExactly("GENERAL", "HEAT", "HOT-WATER");
    }

    @Test
    @DisplayName("배관 케이스: 누수 조항으로 매핑")
    void plumbingCase() {
        assertThat(codes(IssueCategory.PLUMBING, "assistant")).containsExactly("GENERAL", "LEAK");
    }

    @Test
    @DisplayName("코퍼스에 없는 유형(해충): 공통 조항만")
    void uncoveredIssue() {
        assertThat(codes(IssueCategory.PESTS, "assistant")).containsExactly("GENERAL");
    }

    @Test
    @DisplayName("문제 유형 없음(일반 채팅): 쓰는 곳만 맞으면 전부")
    void noIssue() {
        assertThat(codes(null, "assistant")).containsExactly("GENERAL", "HEAT", "HOT-WATER", "LEAK");
    }

    @Test
    @DisplayName("쓰는 곳(used_by)이 다르면 제외")
    void usageFilter() {
        assertThat(codes(IssueCategory.MOLD_MOISTURE, "notice")).containsExactly("GENERAL", "MOLD-NOTICE-ONLY");
    }

    private List<String> codes(IssueCategory issue, String usage) {
        return legalSourceService.findSources(issue, usage).stream().map(LegalSource::code).toList();
    }

    private static LegalDocument document(String code, List<String> issueTypes, List<String> usedBy) {
        return LegalDocument.builder()
                .code(code)
                .label(code)
                .title(code)
                .url("https://example.com/" + code)
                .jurisdiction(Jurisdiction.NYC)
                .issueTypes(issueTypes)
                .usedBy(usedBy)
                .textType(LegalTextType.VERBATIM)
                .summary("summary")
                .build();
    }
}
