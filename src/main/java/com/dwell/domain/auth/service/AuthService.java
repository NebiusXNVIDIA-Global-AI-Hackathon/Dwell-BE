package com.dwell.domain.auth.service;

import com.dwell.domain.auth.dto.request.LoginRequest;
import com.dwell.domain.auth.dto.response.LoginResponse;
import com.dwell.domain.auth.exception.AuthErrorCode;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import com.dwell.global.security.CustomUserDetails;
import com.dwell.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    // 데모 계정 이메일 (V4 마이그레이션으로 생성)
    private static final String DEMO_EMAIL = "demo01@example.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 로그인
    public LoginResponse login(LoginRequest request) {

        // 이메일로 사용자가 존재하는지 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AuthService] Login failed: email not found");
                    return new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        // 비밀번호가 일치하는지 확인
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[AuthService] Login failed: wrong password");
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 로그 출력
        log.info("[AuthService] Login completed: userId={}", user.getId());

        // 응답 반환
        return createLoginResponse(user);
    }

    // Access Token 발급 후 응답 세팅
    private LoginResponse createLoginResponse(User user) {

        // Access Token 발급
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtProvider.createAccessToken(userDetails);

        // 사용자 정보 세팅
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();

        // 응답 세팅
        return LoginResponse.builder()
                .accessToken(accessToken)
                .user(userInfo)
                .build();
    }

    // 데모 계정 로그인
    public LoginResponse demoLogin() {

        // 데모 계정 조회
        User user = userRepository.findByEmail(DEMO_EMAIL)
                .orElseThrow(() -> {
                    log.error("[AuthService] Demo account not found: email={}", DEMO_EMAIL);
                    return new CustomException(AuthErrorCode.DEMO_ACCOUNT_NOT_FOUND);
                });

        // 로그 출력
        log.info("[AuthService] Demo login completed: userId={}", user.getId());

        // 응답 반환
        return createLoginResponse(user);
    }
}
