package com.dwell.domain.user.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.enums.UsState;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.building.service.BuildingService;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.user.dto.request.UserProfileUpdateRequest;
import com.dwell.domain.user.dto.response.UserProfileResponse;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.enums.Role;
import com.dwell.domain.user.exception.UserErrorCode;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private BuildingService buildingService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileService fileService;
    @Mock
    private S3Uploader s3Uploader;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userPlaceRepository, buildingService, passwordEncoder,
                fileRepository, fileService, s3Uploader);
    }

    @Test
    @DisplayName("내 정보 조회: 사진 URL, 주소 포함")
    void getMyProfile() {
        User user = userOf(1L, "old-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(s3Uploader.getPresignedUrl("old-key")).willReturn("https://url");
        UserPlace userPlace = placeOf(user);
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(userPlace));

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.getNickname()).isEqualTo("kjm");
        assertThat(response.getEmail()).isEqualTo("test01@gmail.com");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://url");
        assertThat(response.getAddress()).isEqualTo("254 W 107th St, Apt 4B");
        assertThat(response.isOnboarded()).isTrue();
    }

    @Test
    @DisplayName("내 정보 조회: 사진, 집 정보가 없으면 null, S3 호출 X")
    void getMyProfileWithoutPhoto() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.getProfileImageUrl()).isNull();
        assertThat(response.getAddress()).isNull();
        assertThat(response.isOnboarded()).isFalse();
        verify(s3Uploader, never()).getPresignedUrl(anyString());
    }

    @Test
    @DisplayName("없는 사용자면 USER_NOT_FOUND")
    void userNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 정보 수정: 닉네임, 모국어 변경")
    void updateMyProfile() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("seohu")).willReturn(false);
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        UserProfileResponse response = userService.updateMyProfile(1L, updateRequest("seohu", "es", null));

        assertThat(user.getNickname()).isEqualTo("seohu");
        assertThat(user.getPreferredLanguage()).isEqualTo("es");
        assertThat(response.getNickname()).isEqualTo("seohu");
        verify(fileRepository, never()).findById(any());
    }

    @Test
    @DisplayName("닉네임이 그대로면 중복 조회 X")
    void sameNickname() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.updateMyProfile(1L, updateRequest("kjm", "zh", null));

        verify(userRepository, never()).existsByNickname(anyString());
        assertThat(user.getPreferredLanguage()).isEqualTo("zh");
    }

    @Test
    @DisplayName("다른 사람이 쓰는 닉네임이면 DUPLICATE_NICKNAME")
    void duplicateNickname() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("seohu")).willReturn(true);

        assertThatThrownBy(() -> userService.updateMyProfile(1L, updateRequest("seohu", "en", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.DUPLICATE_NICKNAME);
        assertThat(user.getNickname()).isEqualTo("kjm");
    }

    @Test
    @DisplayName("사진 변경: 이전 사진 삭제 후 새 사진으로 변경")
    void changePhoto() {
        User user = userOf(1L, "old-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        File newFile = fileOf(1L, FilePurpose.PROFILE_IMAGE, "new-key");
        given(fileRepository.findById(120L)).willReturn(Optional.of(newFile));
        File oldFile = mock(File.class);
        given(fileRepository.findByStorageKey("old-key")).willReturn(Optional.of(oldFile));
        given(s3Uploader.getPresignedUrl("new-key")).willReturn("https://new-url");
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        UserProfileResponse response = userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L));

        verify(fileService).deleteFiles(List.of(oldFile));
        assertThat(user.getProfileImageKey()).isEqualTo("new-key");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://new-url");
    }

    @Test
    @DisplayName("첫 사진 등록: 지울 이전 사진 없음")
    void firstPhoto() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        File newFile = fileOf(1L, FilePurpose.PROFILE_IMAGE, "new-key");
        given(fileRepository.findById(120L)).willReturn(Optional.of(newFile));
        given(s3Uploader.getPresignedUrl("new-key")).willReturn("https://new-url");
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L));

        verify(fileService, never()).deleteFiles(any());
        assertThat(user.getProfileImageKey()).isEqualTo("new-key");
    }

    @Test
    @DisplayName("지금 사진과 같은 파일이면 삭제 X")
    void samePhoto() {
        User user = userOf(1L, "same-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        File file = fileOf(1L, FilePurpose.PROFILE_IMAGE, "same-key");
        given(fileRepository.findById(120L)).willReturn(Optional.of(file));
        given(s3Uploader.getPresignedUrl("same-key")).willReturn("https://url");
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L));

        verify(fileService, never()).deleteFiles(any());
        assertThat(user.getProfileImageKey()).isEqualTo("same-key");
    }

    @Test
    @DisplayName("없는 파일이면 FILE_NOT_FOUND")
    void photoNotFound() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(fileRepository.findById(120L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("남의 파일이면 FILE_NOT_FOUND")
    void photoOfAnotherUser() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        File file = fileOf(2L, FilePurpose.PROFILE_IMAGE, "new-key");
        given(fileRepository.findById(120L)).willReturn(Optional.of(file));

        assertThatThrownBy(() -> userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.FILE_NOT_FOUND);
        assertThat(user.getProfileImageKey()).isNull();
    }

    @Test
    @DisplayName("프로필 사진용 파일이 아니면 NOT_PROFILE_IMAGE_FILE")
    void notProfileImageFile() {
        User user = userOf(1L, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        File file = fileOf(1L, FilePurpose.EVIDENCE, "new-key");
        given(fileRepository.findById(120L)).willReturn(Optional.of(file));

        assertThatThrownBy(() -> userService.updateMyProfile(1L, updateRequest("kjm", "en", 120L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(FileErrorCode.NOT_PROFILE_IMAGE_FILE);
        assertThat(user.getProfileImageKey()).isNull();
    }

    // ========== 픽스처 ==========

    private User userOf(Long id, String profileImageKey) {
        User user = User.builder()
                .email("test01@gmail.com")
                .nickname("kjm")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "profileImageKey", profileImageKey);
        return user;
    }

    private UserPlace placeOf(User user) {
        Building building = Building.builder()
                .bbl("1018800031")
                .street("254 W 107th St")
                .city("New York")
                .state(UsState.NY)
                .zip("10025")
                .build();
        return UserPlace.builder()
                .user(user)
                .building(building)
                .unit("Apt 4B")
                .leaseInOwnName(true)
                .build();
    }

    private File fileOf(Long ownerId, FilePurpose purpose, String storageKey) {
        User owner = mock(User.class);
        lenient().when(owner.getId()).thenReturn(ownerId);
        File file = mock(File.class);
        lenient().when(file.getUser()).thenReturn(owner);
        lenient().when(file.getPurpose()).thenReturn(purpose);
        lenient().when(file.getStorageKey()).thenReturn(storageKey);
        return file;
    }

    private UserProfileUpdateRequest updateRequest(String nickname, String preferredLanguage, Long profileImageFileId) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "preferredLanguage", preferredLanguage);
        ReflectionTestUtils.setField(request, "profileImageFileId", profileImageFileId);
        return request;
    }
}
