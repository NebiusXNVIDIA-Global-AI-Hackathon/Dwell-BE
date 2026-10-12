package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.dto.request.CaseCreateRequest;
import com.dwell.domain.cases.dto.response.*;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private final CaseTimelineEventFactory caseTimelineEventFactory;
    private final UserRepository userRepository;
    private final UserPlaceRepository userPlaceRepository;

    public CaseOverviewResponse getCaseOverview(Long caseId, Long userId) {

        log.info("[CaseService] 케이스 상세 조회 - 시작: caseId={}", caseId);

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = findOwnedCase(caseId, userId);

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

    public CaseTimelineResponse getCaseTimeline(Long caseId, Long userId) {

        log.info("[CaseService] 케이스 타임라인 조회 - 시작: caseId={}", caseId);

        // 본인 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = findOwnedCase(caseId, userId);

        // 타임라인 이벤트 + 카드 설명에 쓰는 증거(삭제 제외)·파일·발송된 통지
        List<CaseEvent> events = caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity);
        List<Evidence> evidences = evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(caseEntity);
        List<File> files = evidences.isEmpty() ? List.of() : fileRepository.findAllByEvidenceInOrderByIdAsc(evidences);
        List<Notice> sentNotices = noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity);

        // DTO 변환
        CaseTimelineResponse response = CaseTimelineResponse.builder()
                .header(CaseHeaderResponse.builder()
                        .caseId(caseEntity.getId())
                        .caseNumber(caseEntity.getCaseNumber())
                        .title(caseEntity.getTitle())
                        .address(toAddress(caseEntity))
                        .statusBadge(caseEntity.getStage().toStatusBadge())
                        .severity(caseEntity.getSeverity())
                        .createdAt(caseEntity.getCreatedAt())
                        .build())
                .events(caseTimelineEventFactory.create(events, evidences, files, sentNotices))
                .build();

        log.info("[CaseService] 케이스 타임라인 조회 - 종료: caseId={}, events={}", caseId, response.getEvents().size());

        // DTO 반환
        return response;
    }

    // 본인 케이스 조회 (삭제된 케이스 제외) - 케이스 이벤트 기록에서도 사용
    public Case findOwnedCase(Long caseId, Long userId) {

        // DB에서 케이스 조회 (삭제된 케이스 제외)
        Case caseEntity = caseRepository.findByIdAndDeletedAtIsNull(caseId).orElseThrow(() -> {
            log.warn("[CaseService] 케이스 조회 실패 - 존재하지 않는 케이스 caseId: {}", caseId);
            return new CustomException(CaseErrorCode.CASE_NOT_FOUND);
        });

        // 본인의 케이스가 아니면 X
        if (!caseEntity.getUser().getId().equals(userId)) {
            log.warn("[CaseService] 케이스 조회 실패 - 타인 케이스 접근 시도 caseId: {}, userId: {}", caseId, userId);
            throw new CustomException(CaseErrorCode.NOT_CASE_OWNER);
        }

        return caseEntity;
    }

    // 케이스 생성 (올린 파일들을 증거 1건으로 묶어 함께 저장)
    @Transactional
    public CaseCreateResponse createCase(Long userId, CaseCreateRequest request) {

        // 세부 이슈가 Other면 직접 입력한 값이 있는지 확인
        if (request.getIssueDetail().equals("OTHER") && isBlank(request.getIssueDetailOther())) {
            log.warn("[CaseService] Case creation failed: issue detail (Other) is empty");
            throw new CustomException(CaseErrorCode.OTHER_TEXT_REQUIRED);
        }

        // 공간이 Other면 직접 입력한 값이 있는지 확인
        if (request.getLocation() == Location.OTHER && isBlank(request.getLocationOther())) {
            log.warn("[CaseService] Case creation failed: location (Other) is empty");
            throw new CustomException(CaseErrorCode.OTHER_TEXT_REQUIRED);
        }

        // 부위가 Other면 직접 입력한 값이 있는지 확인
        if (request.getAffectedArea() == AffectedArea.OTHER && isBlank(request.getAffectedAreaOther())) {
            log.warn("[CaseService] Case creation failed: affected area (Other) is empty");
            throw new CustomException(CaseErrorCode.OTHER_TEXT_REQUIRED);
        }

        // Entire Unit은 부위를 고르지 않아야 함
        if (request.getLocation() == Location.ENTIRE_UNIT && request.getAffectedArea() != null) {
            log.warn("[CaseService] Case creation failed: affected area is not allowed for entire unit");
            throw new CustomException(CaseErrorCode.INVALID_AFFECTED_AREA);
        }

        // 나머지 공간은 그 공간에서 고를 수 있는 부위여야 함
        if (request.getLocation() != Location.ENTIRE_UNIT
                && (request.getAffectedArea() == null || !request.getAffectedArea().isAllowedIn(request.getLocation()))) {
            log.warn("[CaseService] Case creation failed: invalid affected area: location={}, affectedArea={}",
                    request.getLocation(), request.getAffectedArea());
            throw new CustomException(CaseErrorCode.INVALID_AFFECTED_AREA);
        }

        // 증거 파일이 1개 이상인지 확인
        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            log.warn("[CaseService] Case creation failed: no evidence file");
            throw new CustomException(CaseErrorCode.EVIDENCE_REQUIRED);
        }

        // 파일 조회 (같은 ID가 여러 번 오면 한 번만)
        List<Long> fileIds = request.getFileIds().stream().distinct().toList();
        List<File> files = fileRepository.findAllById(fileIds);

        // 없는 파일이 섞여 있으면 X
        if (files.size() != fileIds.size()) {
            log.warn("[CaseService] Case creation failed: file not found: fileIds={}", fileIds);
            throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
        }

        for (File file : files) {
            // 남의 파일이면 X (파일이 있는지 드러나지 않게 없는 파일과 같은 에러)
            if (!file.getUser().getId().equals(userId)) {
                log.warn("[CaseService] Case creation failed: file of another user: fileId={}", file.getId());
                throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
            }

            // 증거용으로 올린 파일이 아니면 X (프로필 사진·채팅 첨부 등)
            if (file.getPurpose() != FilePurpose.EVIDENCE) {
                log.warn("[CaseService] Case creation failed: not an evidence file: fileId={}, purpose={}", file.getId(), file.getPurpose());
                throw new CustomException(FileErrorCode.NOT_EVIDENCE_FILE);
            }

            // 이미 다른 증거에 연결된 파일이면 X
            if (file.getEvidence() != null) {
                log.warn("[CaseService] Case creation failed: file already linked: fileId={}", file.getId());
                throw new CustomException(FileErrorCode.FILE_ALREADY_LINKED);
            }
        }

        // 로그인한 사용자 (인증 필터에서 이미 확인했으므로 조회 없이 참조만)
        User user = userRepository.getReferenceById(userId);

        // 사용자의 집 정보 조회 (케이스에 주소 스냅샷으로 저장)
        Building building = null;
        String unit = null;
        UserPlace userPlace = userPlaceRepository.findByUserId(userId).orElse(null);
        if (userPlace != null) {
            building = userPlace.getBuilding();
            unit = userPlace.getUnit();
        }

        // 케이스 번호 생성 (C-001, C-002, ... / 삭제된 케이스도 포함해서 셈)
        long caseCount = caseRepository.countByUserId(userId);
        String caseNumber = String.format("C-%03d", caseCount + 1);

        // 케이스 객체 생성 (제목은 초기값, 심각도는 AI 분석 후에 채움)
        Case caseEntity =
                Case.builder()
                        .caseNumber(caseNumber)
                        .user(user)
                        .building(building)
                        .unit(unit)
                        .title(createTitle(request))
                        .issueCategory(request.getIssueCategory())
                        .issueDetail(request.getIssueDetail())
                        .issueDetailOther(request.getIssueDetailOther())
                        .issueType(toIssueType(request.getIssueCategory()))
                        .location(request.getLocation())
                        .locationOther(request.getLocationOther())
                        .affectedArea(request.getAffectedArea())
                        .affectedAreaOther(request.getAffectedAreaOther())
                        .stage(CaseStage.LOGGED)
                        .visibility(request.getVisibility())
                        .description(request.getDescription())
                        .safetyAcknowledged(request.isSafetyAcknowledged())
                        .analysisStatus(AnalysisStatus.PENDING)
                        .build();

        // DB 저장
        Case savedCase = caseRepository.save(caseEntity);

        // 증거 번호 생성 (EV-001, EV-002, ... 사용자별)
        long evidenceCount = evidenceRepository.countByUserId(userId);
        String evidenceCode = String.format("EV-%03d", evidenceCount + 1);

        // 증거 객체 생성 (케이스 생성 때 올린 파일들을 증거 1건으로 묶음)
        Evidence evidence =
                Evidence.builder()
                        .caseEntity(savedCase)
                        .user(user)
                        .evidenceCode(evidenceCode)
                        .mediaType(toEvidenceMediaType(files))
                        .location(request.getLocation())
                        .locationDetail(request.getLocationOther())
                        .description(request.getDescription())
                        .analysisStatus(AnalysisStatus.PENDING)
                        .build();

        // DB 저장
        Evidence savedEvidence = evidenceRepository.save(evidence);

        // 파일을 증거에 연결 (트랜잭션이 끝날 때 변경 감지로 저장)
        for (File file : files) {
            file.linkEvidence(savedEvidence);
        }

        // 케이스 생성 이벤트 객체 생성 (진행 스텝·타임라인의 "Case Created")
        CaseEvent caseEvent =
                CaseEvent.builder()
                        .caseEntity(savedCase)
                        .type(CaseEventType.CASE_CREATED)
                        .occurredAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                        .title(new LocalizedText(CaseEventType.CASE_CREATED.getLabel(), null))
                        .alert(false)
                        .build();

        // DB 저장
        caseEventRepository.save(caseEvent);

        // TODO AI 분석 비동기 시작 (severity·title·analysis·checklist 생성 후 analysisStatus = DONE)

        // 로그 출력
        log.info("[CaseService] Case created: caseId={}, caseNumber={}, files={}", savedCase.getId(), savedCase.getCaseNumber(), files.size());

        // 응답 세팅
        return CaseCreateResponse.builder()
                .caseId(savedCase.getId())
                .caseNumber(savedCase.getCaseNumber())
                .stage(savedCase.getStage())
                .analysisStatus(savedCase.getAnalysisStatus())
                .build();
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

    // 케이스 제목 생성: "Water Leak in Bathroom · Ceiling" (부위가 없으면 "No Heat in Entire Unit")
    private String createTitle(CaseCreateRequest request) {

        // 세부 이슈 (Other면 직접 입력한 값)
        String issue = toLabel(request.getIssueDetail());
        if (request.getIssueDetail().equals("OTHER")) {
            issue = request.getIssueDetailOther();
        }

        // 공간 (Other면 직접 입력한 값)
        String place = toLabel(request.getLocation().name());
        if (request.getLocation() == Location.OTHER) {
            place = request.getLocationOther();
        }

        String title = issue + " in " + place;

        // 부위 (Entire Unit이면 없음, Other면 직접 입력한 값)
        if (request.getAffectedArea() != null) {
            String area = toLabel(request.getAffectedArea().name());
            if (request.getAffectedArea() == AffectedArea.OTHER) {
                area = request.getAffectedAreaOther();
            }
            title = title + " · " + area;
        }

        // 제목 컬럼 최대 길이 100자
        if (title.length() > 100) {
            title = title.substring(0, 100);
        }

        return title;
    }

    // 카테고리 → 이슈 유형 (AI 분석 결과가 나오면 갱신될 수 있음)
    private IssueType toIssueType(IssueCategory issueCategory) {
        return switch (issueCategory) {
            case WATER_FLOODING -> IssueType.WATER_LEAK;
            case MOLD_MOISTURE -> IssueType.MOLD;
            case HEAT_HOT_WATER -> IssueType.NO_HEAT;
            case PESTS -> IssueType.PEST;
            default -> IssueType.OTHER;
        };
    }

    // 코드 → 표시 문구 (WATER_LEAK → Water Leak)
    private String toLabel(String code) {
        return Arrays.stream(code.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // 증거의 파일 종류: 모두 같으면 그 종류, 섞여 있으면 MULTI
    private MediaType toEvidenceMediaType(List<File> files) {
        MediaType firstType = files.get(0).getMediaType();
        for (File file : files) {
            if (file.getMediaType() != firstType) {
                return MediaType.MULTI;
            }
        }
        return firstType;
    }

    // 값이 비어 있는지 (null 또는 공백만)
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
