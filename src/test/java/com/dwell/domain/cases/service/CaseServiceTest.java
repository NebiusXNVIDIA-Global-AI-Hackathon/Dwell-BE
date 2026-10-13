package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.dto.request.CaseCreateRequest;
import com.dwell.domain.cases.dto.request.CaseEvidenceAddRequest;
import com.dwell.domain.cases.dto.response.CaseCreateResponse;
import com.dwell.domain.cases.dto.response.CaseOptionsResponse;
import com.dwell.domain.cases.dto.response.CaseOverviewResponse;
import com.dwell.domain.cases.dto.response.CaseTimelineResponse;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.*;
import com.dwell.domain.cases.exception.CaseErrorCode;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseChecklistItemRepository caseChecklistItemRepository;
    @Mock
    private CaseEventRepository caseEventRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private FileService fileService;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(caseRepository, caseChecklistItemRepository, caseEventRepository, noticeRepository,
                evidenceRepository, fileRepository, new CaseBannerFactory(), new CaseTimelineStepFactory(), new CaseTimelineEventFactory(),
                userRepository, userPlaceRepository, fileService);
    }

    @Test
    @DisplayName("없는 케이스면 CASE_NOT_FOUND")
    void caseNotFound() {
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.getCaseOverview(6L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CASE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사람 케이스면 NOT_CASE_OWNER")
    void notOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.getCaseOverview(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
    }

    @Test
    @DisplayName("상세 조회: stage에서 status·배지 파생, 주소 조합, 빈 증거·체크리스트")
    void overview() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());
        given(caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(any())).willReturn(List.of());
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(any())).willReturn(List.of());

        CaseOverviewResponse response = caseService.getCaseOverview(6L, 1L);

        assertThat(response.getCaseNumber()).isEqualTo("C-006");
        assertThat(response.getAddress()).isEqualTo("123 Main St, Apt 4B");
        assertThat(response.getStatus()).isEqualTo(CaseStatus.WAITING);
        assertThat(response.getStatusBadge()).isEqualTo(StatusBadge.IN_PROGRESS);
        assertThat(response.getCurrentStatus().getTitle().en()).isEqualTo("Waiting for your landlord's response");
        assertThat(response.getTimelineSteps()).isNotEmpty();
        assertThat(response.getEvidence().getCount()).isZero();
        assertThat(response.getChecklist().getItems()).isEmpty();
    }

    @Test
    @DisplayName("타임라인 조회: 헤더(번호·주소·배지) + 이벤트 카드")
    void timeline() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of(
                CaseBannerFactoryTest.event(caseEntity, CaseEventType.CASE_CREATED, LocalDateTime.of(2026, 9, 28, 20, 12), null)));
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());

        CaseTimelineResponse response = caseService.getCaseTimeline(6L, 1L);

        assertThat(response.getHeader().getCaseNumber()).isEqualTo("C-006");
        assertThat(response.getHeader().getAddress()).isEqualTo("123 Main St, Apt 4B");
        assertThat(response.getHeader().getStatusBadge()).isEqualTo(StatusBadge.IN_PROGRESS);
        assertThat(response.getEvents()).extracting(event -> event.getTitle().en()).containsExactly("Case Created");
    }

    @Test
    @DisplayName("타임라인 조회: 다른 사람 케이스면 NOT_CASE_OWNER")
    void timelineNotOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.getCaseTimeline(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
    }

    @Test
    @DisplayName("케이스 생성: 번호·상태 세팅, 주소 스냅샷, 파일을 증거 1건으로 묶고 Case Created 이벤트 기록")
    void createCase() {
        File photo = fileOf(1L, MediaType.PHOTO);
        File video = fileOf(1L, MediaType.VIDEO);
        stubCaseCreation(List.of(photo, video), "C-005");

        // 집 정보
        Building building = mock(Building.class);
        UserPlace userPlace = mock(UserPlace.class);
        given(userPlace.getBuilding()).willReturn(building);
        given(userPlace.getUnit()).willReturn("Apt 4B");
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(userPlace));

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);
        ReflectionTestUtils.setField(request, "fileIds", List.of(101L, 102L));

        CaseCreateResponse response = caseService.createCase(1L, request);

        // 응답
        assertThat(response.getCaseNumber()).isEqualTo("C-006");
        assertThat(response.getStage()).isEqualTo(CaseStage.LOGGED);
        assertThat(response.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);

        // 저장된 케이스
        ArgumentCaptor<Case> caseCaptor = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCaptor.capture());
        Case savedCase = caseCaptor.getValue();
        assertThat(savedCase.getTitle()).isEqualTo("Water Leak in Bathroom · Ceiling");
        assertThat(savedCase.getIssueType()).isEqualTo(IssueType.WATER_LEAK);
        assertThat(savedCase.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(savedCase.getBuilding()).isEqualTo(building);
        assertThat(savedCase.getUnit()).isEqualTo("Apt 4B");
        assertThat(savedCase.getSeverity()).isNull();

        // 증거 1건 (사진 + 영상이라 MULTI), 파일 2개 모두 연결
        ArgumentCaptor<Evidence> evidenceCaptor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue().getEvidenceCode()).isEqualTo("EV-001");
        assertThat(evidenceCaptor.getValue().getMediaType()).isEqualTo(MediaType.MULTI);
        verify(photo).linkEvidence(evidenceCaptor.getValue());
        verify(video).linkEvidence(evidenceCaptor.getValue());

        // Case Created 이벤트
        ArgumentCaptor<CaseEvent> eventCaptor = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(CaseEventType.CASE_CREATED);
        assertThat(eventCaptor.getValue().getCaseEntity()).isEqualTo(savedCase);
    }

    @Test
    @DisplayName("케이스 생성: Entire Unit은 부위 없이 제목 생성, 집 정보가 없으면 주소 비움")
    void createCaseEntireUnit() {
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), null);

        CaseCreateRequest request = createRequest(IssueCategory.HEAT_HOT_WATER, "NO_HEAT", Location.ENTIRE_UNIT, null);

        CaseCreateResponse response = caseService.createCase(1L, request);

        assertThat(response.getCaseNumber()).isEqualTo("C-001");

        ArgumentCaptor<Case> caseCaptor = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCaptor.capture());
        assertThat(caseCaptor.getValue().getTitle()).isEqualTo("No Heat in Entire Unit");
        assertThat(caseCaptor.getValue().getIssueType()).isEqualTo(IssueType.NO_HEAT);
        assertThat(caseCaptor.getValue().getAffectedArea()).isNull();
        assertThat(caseCaptor.getValue().getBuilding()).isNull();
    }

    @Test
    @DisplayName("케이스 생성: Other를 고르면 직접 입력한 값으로 제목 생성")
    void createCaseWithOtherInputs() {
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), null);

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "OTHER", Location.OTHER, AffectedArea.OTHER);
        ReflectionTestUtils.setField(request, "issueDetailOther", "Rainwater coming in");
        ReflectionTestUtils.setField(request, "locationOther", "Garage");
        ReflectionTestUtils.setField(request, "affectedAreaOther", "Behind the door");

        caseService.createCase(1L, request);

        ArgumentCaptor<Case> caseCaptor = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCaptor.capture());
        assertThat(caseCaptor.getValue().getTitle()).isEqualTo("Rainwater coming in in Garage · Behind the door");
    }

    @Test
    @DisplayName("케이스 생성: 중간 케이스가 지워져도 가장 최근 번호 다음으로 생성")
    void createCaseAfterDeletion() {
        // C-001, C-003이 남아 있음 (C-002 삭제) → 가장 최근은 C-003
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), "C-003");

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);

        CaseCreateResponse response = caseService.createCase(1L, request);

        assertThat(response.getCaseNumber()).isEqualTo("C-004");
    }

    @Test
    @DisplayName("케이스 생성: 카테고리에 없는 세부 이슈면 INVALID_ISSUE_DETAIL")
    void createCaseInvalidIssueDetail() {
        // Gas 카테고리에 Roaches
        CaseCreateRequest request = createRequest(IssueCategory.GAS, "ROACHES", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_ISSUE_DETAIL);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 제목에 와프 문구 사용 (WATER_STAIN → Water Stain or Damage, SHOWER → Shower / Tub)")
    void createCaseTitleUsesLabels() {
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), null);

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_STAIN", Location.BATHROOM, AffectedArea.SHOWER);

        caseService.createCase(1L, request);

        ArgumentCaptor<Case> caseCaptor = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCaptor.capture());
        assertThat(caseCaptor.getValue().getTitle()).isEqualTo("Water Stain or Damage in Bathroom · Shower / Tub");
    }

    @Test
    @DisplayName("입력 옵션: 카테고리 10개, 세부 이슈 마지막은 OTHER, 안전 경고 조건")
    void getCaseOptionsIssueCategories() {
        CaseOptionsResponse response = caseService.getCaseOptions();

        assertThat(response.getIssueCategories()).hasSize(10);

        // Electricity는 카테고리에 ELECTRICAL, Gas는 GAS
        assertThat(findCategory(response, IssueCategory.ELECTRICITY).getSafetyAlert()).isEqualTo(SafetyAlert.ELECTRICAL);
        assertThat(findCategory(response, IssueCategory.GAS).getSafetyAlert()).isEqualTo(SafetyAlert.GAS);

        // Water & Flooding은 카테고리에 경고가 없고, 세부 이슈 Flooding에 FLOODING
        CaseOptionsResponse.IssueCategoryOption waterFlooding = findCategory(response, IssueCategory.WATER_FLOODING);
        assertThat(waterFlooding.getSafetyAlert()).isNull();
        assertThat(waterFlooding.getDetails())
                .filteredOn(detail -> detail.getCode().equals("FLOODING"))
                .extracting(CaseOptionsResponse.IssueDetailOption::getSafetyAlert)
                .containsExactly(SafetyAlert.FLOODING);

        // 세부 이슈 마지막은 직접 입력 (Appliances만 문구가 다름)
        assertThat(waterFlooding.getDetails()).last()
                .extracting(CaseOptionsResponse.IssueDetailOption::getCode).isEqualTo("OTHER");
        assertThat(findCategory(response, IssueCategory.APPLIANCES).getDetails()).last()
                .extracting(CaseOptionsResponse.IssueDetailOption::getLabel).isEqualTo("Other Appliance");

        // 안전 경고 문구 3개
        assertThat(response.getSafetyAlerts()).containsOnlyKeys(SafetyAlert.ELECTRICAL, SafetyAlert.GAS, SafetyAlert.FLOODING);
    }

    @Test
    @DisplayName("입력 옵션: 공간 순서, 공간별 부위, Entire Unit은 부위 없음, 촬영 가이드 4장")
    void getCaseOptionsLocations() {
        CaseOptionsResponse response = caseService.getCaseOptions();

        // 와프 Step 3 순서 (Hallway 제외)
        assertThat(response.getLocations()).extracting(CaseOptionsResponse.LocationOption::getCode)
                .containsExactly(Location.BATHROOM, Location.KITCHEN, Location.LIVING_ROOM, Location.BEDROOM,
                        Location.LAUNDRY_AREA, Location.BALCONY, Location.ENTIRE_UNIT, Location.EXTERIOR, Location.OTHER);

        // Bathroom에는 Shower / Tub가 있고 Sofa는 없음
        CaseOptionsResponse.LocationOption bathroom = response.getLocations().get(0);
        assertThat(bathroom.getAffectedAreas()).extracting(CaseOptionsResponse.AffectedAreaOption::getLabel).contains("Shower / Tub");
        assertThat(bathroom.getAffectedAreas()).extracting(CaseOptionsResponse.AffectedAreaOption::getCode).doesNotContain(AffectedArea.SOFA_SEATING);

        // Entire Unit은 Step 4를 건너뜀
        CaseOptionsResponse.LocationOption entireUnit = response.getLocations().get(6);
        assertThat(entireUnit.getCode()).isEqualTo(Location.ENTIRE_UNIT);
        assertThat(entireUnit.getAffectedAreas()).isEmpty();

        // 촬영 가이드 4장
        assertThat(response.getEvidenceGuide().getShots()).hasSize(4);
    }

    @Test
    @DisplayName("케이스 생성: 세부 이슈가 Other인데 입력값이 없으면 OTHER_TEXT_REQUIRED")
    void createCaseOtherTextMissing() {
        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "OTHER", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.OTHER_TEXT_REQUIRED);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 공간에서 고를 수 없는 부위면 INVALID_AFFECTED_AREA")
    void createCaseInvalidAffectedArea() {
        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.SOFA_SEATING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_AFFECTED_AREA);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: Entire Unit인데 부위를 보내면 INVALID_AFFECTED_AREA")
    void createCaseEntireUnitWithAffectedArea() {
        CaseCreateRequest request = createRequest(IssueCategory.HEAT_HOT_WATER, "NO_HEAT", Location.ENTIRE_UNIT, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_AFFECTED_AREA);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: Entire Unit이 아닌데 부위가 없으면 INVALID_AFFECTED_AREA")
    void createCaseAffectedAreaMissing() {
        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, null);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.INVALID_AFFECTED_AREA);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 파일이 없으면 EVIDENCE_REQUIRED")
    void createCaseEvidenceRequired() {
        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);
        ReflectionTestUtils.setField(request, "fileIds", List.of());

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.EVIDENCE_REQUIRED);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 없는 파일이 섞여 있으면 FILE_NOT_FOUND")
    void createCaseFileNotFound() {
        given(fileRepository.findAllById(any())).willReturn(List.of());

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_NOT_FOUND);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 남의 파일이면 FILE_NOT_FOUND")
    void createCaseFileOfOtherUser() {
        File otherUsersFile = fileOf(2L, MediaType.PHOTO);
        given(fileRepository.findAllById(any())).willReturn(List.of(otherUsersFile));

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_NOT_FOUND);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 이미 다른 증거에 연결된 파일이면 FILE_ALREADY_LINKED")
    void createCaseFileAlreadyLinked() {
        File linkedFile = fileOf(1L, MediaType.PHOTO);
        given(linkedFile.getEvidence()).willReturn(mock(Evidence.class));
        given(fileRepository.findAllById(any())).willReturn(List.of(linkedFile));

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_ALREADY_LINKED);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 생성: 증거용이 아닌 파일이면 NOT_EVIDENCE_FILE")
    void createCaseNotEvidenceFile() {
        File profileImage = fileOf(1L, MediaType.PHOTO);
        given(profileImage.getPurpose()).willReturn(FilePurpose.PROFILE_IMAGE);
        given(fileRepository.findAllById(any())).willReturn(List.of(profileImage));

        CaseCreateRequest request = createRequest(IssueCategory.WATER_FLOODING, "WATER_LEAK", Location.BATHROOM, AffectedArea.CEILING);

        assertThatThrownBy(() -> caseService.createCase(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.NOT_EVIDENCE_FILE);
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("증거 추가: 파일을 증거 1건으로 묶고 Evidence Added 이벤트 기록, 분석 상태 PENDING, 갱신된 상세 반환")
    void addEvidence() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        // 파일 2개
        File photo = fileOf(1L, MediaType.PHOTO);
        File secondPhoto = fileOf(1L, MediaType.PHOTO);
        given(fileRepository.findAllById(any())).willReturn(List.of(photo, secondPhoto));

        // 증거 저장 시 ID 부여
        // 가장 최근 증거가 EV-003 → 다음은 EV-004
        given(evidenceRepository.findTopByUserIdOrderByIdDesc(1L))
                .willReturn(Optional.of(Evidence.builder().evidenceCode("EV-003").build()));
        given(evidenceRepository.save(any(Evidence.class))).willAnswer(invocation -> {
            Evidence saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // 갱신된 상세 조회용
        stubCaseOverview();

        CaseEvidenceAddRequest request = evidenceAddRequest(List.of(110L, 111L), EvidencePhase.PROGRESS, "Ceiling patched but still damp");

        CaseOverviewResponse response = caseService.addEvidence(6L, 1L, request);

        // 갱신된 상세
        assertThat(response.getCaseId()).isEqualTo(6L);

        // 증거 1건 (사진만이라 PHOTO), 단계·메모 저장, 파일 2개 연결
        ArgumentCaptor<Evidence> evidenceCaptor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        Evidence savedEvidence = evidenceCaptor.getValue();
        assertThat(savedEvidence.getEvidenceCode()).isEqualTo("EV-004");
        assertThat(savedEvidence.getMediaType()).isEqualTo(MediaType.PHOTO);
        assertThat(savedEvidence.getPhase()).isEqualTo(EvidencePhase.PROGRESS);
        assertThat(savedEvidence.getDescription()).isEqualTo("Ceiling patched but still damp");
        verify(photo).linkEvidence(savedEvidence);
        verify(secondPhoto).linkEvidence(savedEvidence);

        // Evidence Added 이벤트 (증거 ID 참조)
        ArgumentCaptor<CaseEvent> eventCaptor = ArgumentCaptor.forClass(CaseEvent.class);
        verify(caseEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(CaseEventType.EVIDENCE_ADDED);
        assertThat(eventCaptor.getValue().getRefType()).isEqualTo(CaseEventRefType.EVIDENCE);
        assertThat(eventCaptor.getValue().getRefId()).isEqualTo(10L);

        // 재분석 대기
        assertThat(caseEntity.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    @DisplayName("증거 추가: phase를 생략하면 INITIAL")
    void addEvidenceDefaultPhase() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        File photo = fileOf(1L, MediaType.PHOTO);
        given(fileRepository.findAllById(any())).willReturn(List.of(photo));
        given(evidenceRepository.findTopByUserIdOrderByIdDesc(1L)).willReturn(Optional.empty());
        given(evidenceRepository.save(any(Evidence.class))).willAnswer(invocation -> invocation.getArgument(0));
        stubCaseOverview();

        caseService.addEvidence(6L, 1L, evidenceAddRequest(List.of(110L), null, null));

        ArgumentCaptor<Evidence> evidenceCaptor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue().getPhase()).isEqualTo(EvidencePhase.INITIAL);
    }

    @Test
    @DisplayName("증거 추가: 파일이 없으면 EVIDENCE_REQUIRED")
    void addEvidenceRequired() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.addEvidence(6L, 1L, evidenceAddRequest(List.of(), null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.EVIDENCE_REQUIRED);
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("증거 추가: 다른 사람 케이스면 NOT_CASE_OWNER")
    void addEvidenceNotOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.addEvidence(6L, 2L, evidenceAddRequest(List.of(110L), null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("증거 추가: 이미 다른 증거에 연결된 파일이면 FILE_ALREADY_LINKED")
    void addEvidenceFileAlreadyLinked() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        File linkedFile = fileOf(1L, MediaType.PHOTO);
        given(linkedFile.getEvidence()).willReturn(mock(Evidence.class));
        given(fileRepository.findAllById(any())).willReturn(List.of(linkedFile));

        assertThatThrownBy(() -> caseService.addEvidence(6L, 1L, evidenceAddRequest(List.of(110L), null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_ALREADY_LINKED);
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("케이스 삭제: 증거에 연결된 파일을 지운 뒤 케이스 삭제")
    void deleteCase() {
        Case caseEntity = caseOf(owner(1L), CaseStage.AWAITING_RESPONSE);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        // 케이스의 증거 파일 2개
        File photo = fileOf(1L, MediaType.PHOTO);
        File video = fileOf(1L, MediaType.VIDEO);
        given(fileRepository.findAllByEvidenceCaseEntity(caseEntity)).willReturn(List.of(photo, video));

        caseService.deleteCase(6L, 1L);

        // 파일을 먼저 지우고, 그다음 케이스 삭제
        org.mockito.InOrder inOrder = inOrder(fileService, caseRepository);
        inOrder.verify(fileService).deleteFiles(List.of(photo, video));
        inOrder.verify(caseRepository).delete(caseEntity);
    }

    @Test
    @DisplayName("케이스 삭제: 증거가 없으면 빈 파일 목록으로 삭제")
    void deleteCaseWithoutEvidence() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));
        given(fileRepository.findAllByEvidenceCaseEntity(caseEntity)).willReturn(List.of());

        caseService.deleteCase(6L, 1L);

        verify(fileService).deleteFiles(List.of());
        verify(caseRepository).delete(caseEntity);
    }

    @Test
    @DisplayName("케이스 삭제: 없는 케이스면 CASE_NOT_FOUND")
    void deleteCaseNotFound() {
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.deleteCase(6L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.CASE_NOT_FOUND);
        verify(caseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("케이스 삭제: 다른 사람 케이스면 NOT_CASE_OWNER")
    void deleteCaseNotOwner() {
        Case caseEntity = caseOf(owner(1L), CaseStage.LOGGED);
        given(caseRepository.findByIdAndDeletedAtIsNull(6L)).willReturn(Optional.of(caseEntity));

        assertThatThrownBy(() -> caseService.deleteCase(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CaseErrorCode.NOT_CASE_OWNER);
        verify(caseRepository, never()).delete(any());
        verify(fileService, never()).deleteFiles(any());
    }

    // ========== 테스트 픽스처 ==========

    // 입력 옵션에서 카테고리 찾기
    private CaseOptionsResponse.IssueCategoryOption findCategory(CaseOptionsResponse response, IssueCategory category) {
        return response.getIssueCategories().stream()
                .filter(option -> option.getCode() == category)
                .findFirst()
                .orElseThrow();
    }

    private User owner(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private Case caseOf(User user, CaseStage stage) {
        Building building = mock(Building.class);
        lenient().when(building.getStreet()).thenReturn("123 Main St");

        Case caseEntity = Case.builder()
                .caseNumber("C-006")
                .user(user)
                .building(building)
                .unit("Apt 4B")
                .title("Water Leak in Bathroom · Ceiling")
                .stage(stage)
                .build();
        ReflectionTestUtils.setField(caseEntity, "id", 6L);
        return caseEntity;
    }

    // 케이스 생성 요청 (DTO에 setter가 없어서 필드에 직접 값 세팅, 파일 1개·비공개 기본)
    private CaseCreateRequest createRequest(IssueCategory issueCategory, String issueDetail, Location location, AffectedArea affectedArea) {
        CaseCreateRequest request = new CaseCreateRequest();
        ReflectionTestUtils.setField(request, "issueCategory", issueCategory);
        ReflectionTestUtils.setField(request, "issueDetail", issueDetail);
        ReflectionTestUtils.setField(request, "location", location);
        ReflectionTestUtils.setField(request, "affectedArea", affectedArea);
        ReflectionTestUtils.setField(request, "fileIds", List.of(101L));
        ReflectionTestUtils.setField(request, "visibility", Visibility.PRIVATE);
        return request;
    }

    // 업로드된 증거 파일 (아직 증거에 연결되지 않은 상태)
    private File fileOf(Long ownerId, MediaType mediaType) {
        User owner = mock(User.class);
        lenient().when(owner.getId()).thenReturn(ownerId);
        File file = mock(File.class);
        lenient().when(file.getUser()).thenReturn(owner);
        lenient().when(file.getMediaType()).thenReturn(mediaType);
        lenient().when(file.getPurpose()).thenReturn(FilePurpose.EVIDENCE);
        return file;
    }

    // 케이스 생성 성공에 필요한 저장소 동작 (가장 최근 케이스 번호가 없으면 첫 케이스)
    private void stubCaseCreation(List<File> files, String latestCaseNumber) {
        Optional<Case> latestCase = Optional.empty();
        if (latestCaseNumber != null) {
            latestCase = Optional.of(Case.builder().caseNumber(latestCaseNumber).build());
        }

        given(fileRepository.findAllById(any())).willReturn(files);
        given(userRepository.getReferenceById(1L)).willReturn(mock(User.class));
        given(caseRepository.findTopByUserIdOrderByIdDesc(1L)).willReturn(latestCase);
        given(caseRepository.save(any(Case.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(evidenceRepository.findTopByUserIdOrderByIdDesc(1L)).willReturn(Optional.empty());
        given(evidenceRepository.save(any(Evidence.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    // 증거 추가 요청 (DTO에 setter가 없어서 필드에 직접 값 세팅)
    private CaseEvidenceAddRequest evidenceAddRequest(List<Long> fileIds, EvidencePhase phase, String note) {
        CaseEvidenceAddRequest request = new CaseEvidenceAddRequest();
        ReflectionTestUtils.setField(request, "fileIds", fileIds);
        ReflectionTestUtils.setField(request, "phase", phase);
        ReflectionTestUtils.setField(request, "note", note);
        return request;
    }

    // 갱신된 케이스 상세 조회에 필요한 저장소 동작 (이벤트·통지·체크리스트·증거 모두 비어 있음)
    private void stubCaseOverview() {
        given(caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(any())).willReturn(List.of());
        given(noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(any())).willReturn(List.of());
        given(caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(any())).willReturn(List.of());
        given(evidenceRepository.findAllByCaseEntityAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(any())).willReturn(List.of());
    }
}
