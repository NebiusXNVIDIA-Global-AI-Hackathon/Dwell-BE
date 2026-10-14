package com.dwell.domain.auth.service;

import com.dwell.domain.auth.dto.request.LoginRequest;
import com.dwell.domain.auth.dto.request.RefreshTokenRequest;
import com.dwell.domain.auth.dto.response.LoginResponse;
import com.dwell.domain.auth.dto.response.RefreshTokenResponse;
import com.dwell.domain.auth.exception.AuthErrorCode;
import com.dwell.domain.user.entity.RefreshToken;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.RefreshTokenRepository;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import com.dwell.global.security.CustomUserDetails;
import com.dwell.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

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
    private final RefreshTokenRepository refreshTokenRepository;

    // 로그인
    @Transactional
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

    // 데모 계정 로그인
    @Transactional
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

    // 토큰 재발급 (사용한 Refresh Token은 폐기하고 새로 발급)
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {

        // DB에서 Refresh Token 조회 (DB에는 해시로 저장되어 있어서 해시로 조회)
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                .orElseThrow(() -> {
                    log.warn("[AuthService] Token refresh failed: refresh token not found");
                    return new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
                });

        // 재발급·로그아웃으로 이미 폐기된 토큰이면 X
        if (refreshToken.getRevokedAt() != null) {
            log.warn("[AuthService] Token refresh failed: revoked refresh token: refreshTokenId={}", refreshToken.getId());
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 만료된 토큰이면 X
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService] Token refresh failed: expired refresh token: refreshTokenId={}", refreshToken.getId());
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용한 Refresh Token 폐기 (변경 감지로 저장)
        refreshToken.revoke();

        // 새 Access Token, Refresh Token 발급
        User user = refreshToken.getUser();
        String accessToken = jwtProvider.createAccessToken(new CustomUserDetails(user));
        String newRefreshToken = issueRefreshToken(user);

        // 로그 출력
        log.info("[AuthService] Token refreshed: userId={}", user.getId());

        // 응답 세팅
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ========== 헬퍼 함수 ==========

    // Access Token, Refresh Token 발급 후 응답 세팅 (로그인, 데모 로그인 공통)
    private LoginResponse createLoginResponse(User user) {

        // Access Token 발급
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtProvider.createAccessToken(userDetails);

        // Refresh Token 발급 (DB 저장)
        String refreshToken = issueRefreshToken(user);

        // 사용자 정보 세팅
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();

        // 응답 세팅
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }

    // Refresh Token 발급 (원본은 응답으로만 주고 DB에는 해시만 저장)
    private String issueRefreshToken(User user) {

        // Refresh Token 생성 (랜덤 문자열)
        String refreshToken = jwtProvider.createRefreshToken();

        // Refresh Token 객체 생성
        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(hash(refreshToken))
                        .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProvider.getRefreshTokenExpiration())))
                        .build();

        // DB 저장
        refreshTokenRepository.save(refreshTokenEntity);

        return refreshToken;
    }

    // 토큰의 SHA-256 해시 (16진수 64자, DB가 유출돼도 원본 토큰은 알 수 없음)
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
