package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.cases.dto.response.CaseEvidenceResponse;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.dto.response.ChecklistItemResponse;
import com.dwell.domain.cases.dto.response.ChecklistResponse;
import com.dwell.domain.cases.dto.response.EvidenceItemResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseChecklistItemRepository caseChecklistItemRepository;
    private final CaseEventRepository caseEventRepository;
    private final NoticeRepository noticeRepository;
    private final EvidenceRepository evidenceRepository;
    private final FileRepository fileRepository;
    private final CaseBannerFactory caseBannerFactory;
    private final CaseTimelineStepFactory caseTimelineStepFactory;

    public CaseOverviewResponse getCaseOverview(Long caseId, Long userId) {

        log.info("[CaseService] 케이스 상세 조회 - 시작: caseId={}", caseId);

        // DB에서 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(caseId).orElseThrow(() -> {
            log.warn("[CaseService] 케이스 조회 실패 - 존재하지 않는 케이스 caseId: {}", caseId);
            return new CustomException(CaseErrorCode.CASE_NOT_FOUND);
        });

        // 본인의 케이스가 아니면 조회 X
        if (!caseEntity.getUser().getId().equals(userId)) {
            log.warn("[CaseService] 케이스 조회 실패 - 타인 케이스 조회 시도 caseId: {}, userId: {}", caseId, userId);
            throw new CustomException(CaseErrorCode.NOT_CASE_OWNER);
        }

        // 배너·진행 스텝 계산에 필요한 타임라인 이벤트, 발송된 통지
        List<CaseEvent> events = caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity);
        List<Notice> sentNotices = noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity);

        // DB 시각과 같은 JVM 기본 시간대 기준 현재 시각
        LocalDateTime now = LocalDateTime.now();

        // DTO 변환
        CaseOverviewResponse response = CaseOverviewResponse.builder()
                .caseId(caseEntity.getId())
                .caseNumber(caseEntity.getCaseNumber())
                .title(caseEntity.getTitle())
                .address(toAddress(caseEntity))
                .issueCategory(caseEntity.getIssueCategory())
                .issueDetail(caseEntity.getIssueDetail())
                .location(caseEntity.getLocation())
                .affectedArea(caseEntity.getAffectedArea())
                .severity(caseEntity.getSeverity())
                .visibility(caseEntity.getVisibility())
                .status(caseEntity.getStage().getStatus())
                .stage(caseEntity.getStage())
                .statusBadge(caseEntity.getStage().toStatusBadge())
                .analysisStatus(caseEntity.getAnalysisStatus())
                .createdAt(caseEntity.getCreatedAt())
                .checklist(toChecklistResponse(caseEntity))
                .currentStatus(caseBannerFactory.create(caseEntity, events, sentNotices, now))
                .timelineSteps(caseTimelineStepFactory.create(caseEntity, events))
                .evidence(toCaseEvidenceResponse(caseEntity))
                .build();

        log.info("[CaseService] 케이스 상세 조회 - 종료: caseId={}, stage={}", caseId, caseEntity.getStage());

        // DTO 반환
        return response;
    }

    // ========== 헬퍼 함수 ==========

    // 와프 형식: "123 Main St, Apt 4B" (unit은 "Apt 4B" 형태로 저장됨)
    private String toAddress(Case caseEntity) {
        Building building = caseEntity.getBuilding();
        if (building == null) {
            return null;
        }
        if (caseEntity.getUnit() == null || caseEntity.getUnit().isBlank()) {
            return building.getStreet();
        }
        return building.getStreet() + ", " + caseEntity.getUnit();
    }

    // 체크리스트 DTO 변환 메소드
    private ChecklistResponse toChecklistResponse(Case caseEntity) {

        List<CaseChecklistItem> items = caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(caseEntity);

        List<ChecklistItemResponse> itemResponses = new ArrayList<>();
        for (CaseChecklistItem item : items) {
            itemResponses.add(ChecklistItemResponse.builder()
                    .itemId(item.getId())
                    .text(new LocalizedText(item.getText(), item.getTextNative()))
                    .checked(item.isChecked())
                    .source(item.getSource())
                    .build());
        }

        return ChecklistResponse.builder()
                .items(itemResponses)
                .build();
    }

    // 증거 요약 DTO 변환 메소드 - 증거 1건 = 파일 여러 개 (와프 폴더 카드 + "3items")
    private CaseEvidenceResponse toCaseEvidenceResponse(Case caseEntity) {

        List<Evidence> evidences = evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity);

        // 증거별 파일 묶기 (쿼리 1번)
        Map<Long, List<File>> filesByEvidenceId = new HashMap<>();
        if (!evidences.isEmpty()) {
            for (File file : fileRepository.findAllByEvidenceInOrderByIdAsc(evidences)) {
                filesByEvidenceId.computeIfAbsent(file.getEvidence().getId(), id -> new ArrayList<>()).add(file);
            }
        }

        List<EvidenceItemResponse> itemResponses = new ArrayList<>();
        for (Evidence evidence : evidences) {
            List<File> files = filesByEvidenceId.getOrDefault(evidence.getId(), List.of());

            itemResponses.add(EvidenceItemResponse.builder()
                    .evidenceId(evidence.getId())
                    .mediaType(evidence.getMediaType())
                    // TODO 파일 URL 생성 방식(Object Storage public URL / presigned) 확정 후 교체. 지금은 storageKey
                    .thumbnailUrl(files.isEmpty() ? null : files.get(0).getStorageKey())
                    .itemCount(files.size())
                    .phase(evidence.getPhase())
                    .createdAt(evidence.getCreatedAt())
                    .build());
        }

        return CaseEvidenceResponse.builder()
                .count(itemResponses.size())
                .items(itemResponses)
                .build();
    }
}
