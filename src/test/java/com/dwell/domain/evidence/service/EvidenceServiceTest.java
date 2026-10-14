package com.dwell.domain.evidence.service;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.dto.response.EvidenceDetailResponse;
import com.dwell.domain.evidence.entity.Evidence;
import com.dwell.domain.evidence.exception.EvidenceErrorCode;
import com.dwell.domain.evidence.repository.EvidenceRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.common.enums.Severity;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private EvidenceService evidenceService;

    private Evidence evidence;
    private File photo;
    private File audio;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L);
        Case caseEntity = mock(Case.class);
        lenient().when(caseEntity.getId()).thenReturn(6L);

        evidence = Evidence.builder().caseEntity(caseEntity).user(user).evidenceCode("EV-006").mediaType(MediaType.MULTI)
                .location(Location.BATHROOM).analysisStatus(AnalysisStatus.PENDING).build();
        ReflectionTestUtils.setField(evidence, "id", 6L);

        photo = File.builder().publicId("f_1").user(user).mediaType(MediaType.PHOTO).mimeType("image/jpeg")
                .storageKey("evidence/k1").sha256("9f2b").build();
        audio = File.builder().publicId("f_2").user(user).mediaType(MediaType.AUDIO).mimeType("audio/m4a")
                .storageKey("evidence/k2").sha256("41ac").durationSec(58).build();
        lenient().when(s3Uploader.getPresignedUrl("evidence/k1")).thenReturn("https://s3/k1");
    }

    @Test
    @DisplayName("분석 완료된 증거: 파일·해시·개별 판정·무결성 체크")
    void getEvidence() {
        evidence.applyAnalysis(IssueType.WATER_LEAK, Severity.MEDIUM, List.of(new LocalizedText("Dry stain", "마른 얼룩")),
                "ceiling", null, null);
        given(evidenceRepository.findById(6L)).willReturn(Optional.of(evidence));
        given(fileRepository.findAllByEvidenceInOrderByIdAsc(List.of(evidence))).willReturn(List.of(photo, audio));

        EvidenceDetailResponse response = evidenceService.getEvidence(6L, 1L);

        assertThat(response.getCaseId()).isEqualTo(6L);
        assertThat(response.getMediaSummary()).isEqualTo("1 photo · 1 audio");
        assertThat(response.getLocationLabel()).isEqualTo("Bathroom · ceiling");
        assertThat(response.getFiles()).extracting(EvidenceDetailResponse.FileItem::getSha256).containsExactly("9f2b", "41ac");
        assertThat(response.getFiles().getFirst().getUrl()).isEqualTo("https://s3/k1");
        assertThat(response.getAnalysis().getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(response.getIntegrity().isOriginalSaved()).isTrue();
        assertThat(response.getIntegrity().isAiAssessmentAttached()).isTrue();
        assertThat(response.getIntegrity().isTamperProtected()).isTrue();
    }

    @Test
    @DisplayName("분석 전이면 analysis = null")
    void pending() {
        given(evidenceRepository.findById(6L)).willReturn(Optional.of(evidence));
        given(fileRepository.findAllByEvidenceInOrderByIdAsc(List.of(evidence))).willReturn(List.of(photo));

        EvidenceDetailResponse response = evidenceService.getEvidence(6L, 1L);

        assertThat(response.getAnalysis()).isNull();
        assertThat(response.getIntegrity().isAiAssessmentAttached()).isFalse();
        assertThat(response.getLocationLabel()).isEqualTo("Bathroom");
    }

    @Test
    @DisplayName("남의 증거면 403, 없는 증거면 404")
    void notOwner() {
        given(evidenceRepository.findById(6L)).willReturn(Optional.of(evidence));
        given(evidenceRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.getEvidence(6L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(EvidenceErrorCode.NOT_EVIDENCE_OWNER);
        assertThatThrownBy(() -> evidenceService.getEvidence(7L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(EvidenceErrorCode.EVIDENCE_NOT_FOUND);
    }

    @Test
    @DisplayName("파일 구성 요약: 같은 종류는 복수형")
    void mediaSummary() {
        assertThat(EvidenceService.toMediaSummary(List.of(photo, photo, audio))).isEqualTo("2 photos · 1 audio");
    }
}
