package com.dwell.domain.legal.service;

import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.repository.LegalDocumentRepository;
import com.dwell.global.common.enums.IssueCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
법률 근거 검색 (Legal RAG의 검색 단계)
- 임베딩 모델이 없어서 벡터 검색 대신 문제 유형(issue_types) + 쓰는 곳(used_by)으로 거름
- 코퍼스가 26개라 전부 읽어서 메모리에서 거름
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalSourceService {

    public static final String USAGE_ASSISTANT = "assistant";

    private static final String ALL_ISSUES = "all";

    private final LegalDocumentRepository legalDocumentRepository;

    // 문제 유형·쓰는 곳에 맞는 근거 조회 (issue가 null이면 유형 제한 없음)
    public List<LegalSource> findSources(IssueCategory issue, String usage) {
        List<String> issueTypes = issue == null ? null : corpusIssueTypes(issue);
        return legalDocumentRepository.findAll(Sort.by("id")).stream()
                .filter(document -> document.getUsedBy().contains(usage))
                .filter(document -> issueTypes == null
                        || document.getIssueTypes().contains(ALL_ISSUES)
                        || document.getIssueTypes().stream().anyMatch(issueTypes::contains))
                .map(LegalSource::from)
                .toList();
    }

    // 케이스 문제 유형 -> 코퍼스 issue_types (leak | mold | heat | hot_water), 코퍼스에 없는 유형은 공통(all) 조항만
    static List<String> corpusIssueTypes(IssueCategory issue) {
        return switch (issue) {
            case HEAT_HOT_WATER -> List.of("heat", "hot_water");
            case WATER_FLOODING, PLUMBING -> List.of("leak");
            case MOLD_MOISTURE -> List.of("mold");
            default -> List.of();
        };
    }
}
