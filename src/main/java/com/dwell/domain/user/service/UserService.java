package com.dwell.domain.user.service;

import com.dwell.domain.auth.service.AuthService;
import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.InspectionResultRepository;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.building.service.BuildingService;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.community.repository.PostImageRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.exception.FileErrorCode;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.user.dto.request.UserProfileUpdateRequest;
import com.dwell.domain.user.dto.request.UserSignUpRequest;
import com.dwell.domain.user.dto.response.UserProfileResponse;
import com.dwell.domain.user.dto.response.UserSignUpResponse;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.enums.Role;
import com.dwell.domain.user.exception.UserErrorCode;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final BuildingService buildingService;
    private final PasswordEncoder passwordEncoder;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final S3Uploader s3Uploader;
    private final CaseRepository caseRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final PostImageRepository postImageRepository;

    // 회원가입
    @Transactional
    public UserSignUpResponse signUp(UserSignUpRequest request) {

        // 비밀번호와 비밀번호 확인이 일치하는지 조회
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            log.warn("[UserService] Passwords do not match");
            throw new CustomException(UserErrorCode.PASSWORD_MISMATCH);
        }

        // 닉네임이 중복되는지 조회
        if (userRepository.existsByNickname(request.getNickname())) {
            log.warn("[UserService] Duplicate nickname: nickname={}", request.getNickname());
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 중복된 이메일인지 조회
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[UserService] Duplicate email: email={}", request.getEmail());
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 사용자 객체 생성
        User user = User.builder()
                        .email(request.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .nickname(request.getNickname())
                        .role(Role.USER)
                        .build();

        // DB 저장
        User savedUser = userRepository.save(user);

        // BBL로 건물 조회, 없으면 새로 생성
        Building building = buildingService.findOrCreate(
                request.getBbl(),
                request.getStreet(),
                request.getCity(),
                request.getState(),
                request.getZip()
        );

        // 사용자 집 정보 객체 생성 (본인 명의 계약 여부를 받는 화면이 없어 우선 true)
        UserPlace userPlace =
                UserPlace.builder()
                        .user(savedUser)
                        .building(building)
                        .unit(request.getUnit())
                        .leaseInOwnName(true)
                        .build();

        // DB 저장
        userPlaceRepository.save(userPlace);

        // 로그 출력
        log.info("[UserService] Sign-up completed: userId={}", savedUser.getId());

        // 응답 세팅
        return UserSignUpResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .role(savedUser.getRole())
                .street(building.getStreet())
                .unit(userPlace.getUnit())
                .city(building.getCity())
                .state(building.getState())
                .zip(building.getZip())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // 이메일 사용 가능 여부 조회 (중복이 아니면 true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // 닉네임 사용 가능 여부 조회 (중복이 아니면 true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // 내 정보 조회
    public UserProfileResponse getMyProfile(Long userId) {

        // DB에서 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("[UserService] Profile lookup failed: user not found: userId={}", userId);
            return new CustomException(UserErrorCode.USER_NOT_FOUND);
        });

        // 프로필 사진 URL (버킷이 비공개라 짧게 유효한 URL, 사진이 없으면 null)
        String profileImageUrl = null;
        if (user.getProfileImageKey() != null) {
            profileImageUrl = s3Uploader.getPresignedUrl(user.getProfileImageKey());
        }

        // 사용자의 집 정보 조회 (집 정보가 없으면 null)
        String address = null;
        UserPlace userPlace = userPlaceRepository.findByUserId(userId).orElse(null);
        if (userPlace != null) {
            address = userPlace.getBuilding().getStreet();
            if (userPlace.getUnit() != null && !userPlace.getUnit().isBlank()) {
                address = address + ", " + userPlace.getUnit();
            }
        }

        // 응답 세팅
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .preferredLanguage(user.getPreferredLanguage())
                .profileImageUrl(profileImageUrl)
                .address(address)
                .onboarded(userPlace != null) // 집 정보가 있으면 온보딩 완료
                .build();
    }

    // 내 정보 수정 (Save 시 폼 전체 전송, 이메일은 수정 불가)
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {

        // DB에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserService] Profile update failed: user not found: userId={}", userId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        // 닉네임이 바뀌었으면 중복 조회 (그대로 저장하면 본인 닉네임과 겹쳐서 바뀌었을 때만)
        if (!request.getNickname().equals(user.getNickname()) && userRepository.existsByNickname(request.getNickname())) {
            log.warn("[UserService] Profile update failed: duplicate nickname: nickname={}", request.getNickname());
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 프로필 사진을 바꿨으면 사진 변경 (비어 있으면 지금 사진 유지)
        if (request.getProfileImageFileId() != null) {

            // DB에서 파일 조회
            File file = fileRepository.findById(request.getProfileImageFileId()).orElseThrow(() -> {
                log.warn("[UserService] Profile update failed: file not found: fileId={}", request.getProfileImageFileId());
                return new CustomException(FileErrorCode.FILE_NOT_FOUND);
            });

            // 남의 파일이면 X (파일이 있는지 드러나지 않게 없는 파일과 같은 에러)
            if (!file.getUser().getId().equals(userId)) {
                log.warn("[UserService] Profile update failed: file of another user: fileId={}", file.getId());
                throw new CustomException(FileErrorCode.FILE_NOT_FOUND);
            }

            // 프로필 사진용으로 올린 파일이 아니면 X (증거·게시글 이미지 등)
            if (file.getPurpose() != FilePurpose.PROFILE_IMAGE) {
                log.warn("[UserService] Profile update failed: not a profile image file: fileId={}, purpose={}", file.getId(), file.getPurpose());
                throw new CustomException(FileErrorCode.NOT_PROFILE_IMAGE_FILE);
            }

            // 지금 사진과 다를 때만 변경
            if (!file.getStorageKey().equals(user.getProfileImageKey())) {

                // 이전 사진 삭제 (DB는 바로, S3 원본은 커밋 뒤에)
                if (user.getProfileImageKey() != null) {
                    File oldFile = fileRepository.findByStorageKey(user.getProfileImageKey()).orElse(null);
                    if (oldFile != null) {
                        fileService.deleteFiles(List.of(oldFile));
                    }
                }

                // 프로필 사진 변경 (변경 감지로 저장)
                user.changeProfileImage(file.getStorageKey());
            }
        }

        // 닉네임, 모국어 변경 (변경 감지로 저장)
        user.updateProfile(request.getNickname(), request.getPreferredLanguage());

        // 로그 출력
        log.info("[UserService] Profile updated: userId={}", userId);

        // 응답 반환
        return getMyProfile(userId);
    }

    // 회원 탈퇴 (Hard Delete 방식 : 케이스·증거·파일·채팅·Refresh Token 전부, 커뮤니티 글·댓글은 Deleted user로 유지)
    @Transactional
    public void deleteMyAccount(Long userId) {

        // DB에서 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("[UserService] Account deletion failed: user not found: userId={}", userId);
            return new CustomException(UserErrorCode.USER_NOT_FOUND);
        });

        // 데모 계정이면 X (지우면 모든 사람의 데모 로그인이 깨짐)
        if (AuthService.DEMO_EMAIL.equals(user.getEmail())) {
            log.warn("[UserService] Account deletion failed: demo account: userId={}", userId);
            throw new CustomException(UserErrorCode.DEMO_ACCOUNT_CANNOT_BE_DELETED);
        }

        // 사용자가 올린 파일 전부 조회 (증거·프로필 사진·게시글 이미지·연결 안 된 업로드)
        List<File> files = fileRepository.findAllByUserId(userId);

        // 게시글 이미지 연결 삭제 (게시글은 남기고 사진만 제거, 파일보다 먼저 지워야 FK 에러가 안 남)
        postImageRepository.deleteAllByFileUserId(userId);

        // 파일 삭제 (DB는 바로, S3 원본은 커밋 뒤에)
        fileService.deleteFiles(files);

        // Check My Unit 결과 삭제 (연결된 채팅방은 DB의 ON DELETE CASCADE로 함께 삭제)
        inspectionResultRepository.deleteAllByUserId(userId);

        // 케이스 삭제 (증거·타임라인·체크리스트·Notice·검증·케이스 채팅은 DB의 ON DELETE CASCADE로 함께 삭제)
        caseRepository.deleteAllByUserId(userId);

        // 사용자 삭제 (Refresh Token·기기·집 정보·알림·채팅·좋아요는 CASCADE, 커뮤니티 글·댓글은 작성자만 NULL → Deleted user)
        userRepository.delete(user);

        // TODO : Building Signal·Insight는 다음 재계산 때 남아 있는 PUBLIC 케이스만 세면 자동으로 제외

        // 로그 출력
        log.info("[UserService] Account deleted: userId={}, files={}", userId, files.size());
    }
}
