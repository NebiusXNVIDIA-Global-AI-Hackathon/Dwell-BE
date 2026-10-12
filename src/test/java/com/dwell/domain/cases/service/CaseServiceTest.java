package com.dwell.domain.cases.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.dto.request.CaseCreateRequest;
import com.dwell.domain.cases.dto.response.CaseCreateResponse;
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
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
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

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(caseRepository, caseChecklistItemRepository, caseEventRepository, noticeRepository,
                evidenceRepository, fileRepository, new CaseBannerFactory(), new CaseTimelineStepFactory(), new CaseTimelineEventFactory(),
                userRepository, userPlaceRepository);
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
        stubCaseCreation(List.of(photo, video), 5L);

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
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), 0L);

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
        stubCaseCreation(List.of(fileOf(1L, MediaType.PHOTO)), 0L);

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

    // ========== 테스트 픽스처 ==========

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

    // 케이스 생성 성공에 필요한 저장소 동작
    private void stubCaseCreation(List<File> files, long caseCount) {
        given(fileRepository.findAllById(any())).willReturn(files);
        given(userRepository.getReferenceById(1L)).willReturn(mock(User.class));
        given(caseRepository.countByUserId(1L)).willReturn(caseCount);
        given(caseRepository.save(any(Case.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(evidenceRepository.countByUserId(1L)).willReturn(0L);
        given(evidenceRepository.save(any(Evidence.class))).willAnswer(invocation -> invocation.getArgument(0));
    }
}
