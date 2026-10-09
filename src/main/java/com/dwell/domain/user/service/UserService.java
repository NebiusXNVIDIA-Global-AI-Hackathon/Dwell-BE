package com.dwell.domain.user.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.building.service.BuildingService;
import com.dwell.domain.user.dto.request.UserSignUpRequest;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final BuildingService buildingService;
    private final PasswordEncoder passwordEncoder;

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
}
