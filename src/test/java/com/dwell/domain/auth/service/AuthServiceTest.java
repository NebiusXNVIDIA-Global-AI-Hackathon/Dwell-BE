package com.dwell.domain.auth.service;

import com.dwell.domain.auth.dto.request.LoginRequest;
import com.dwell.domain.auth.dto.request.RefreshTokenRequest;
import com.dwell.domain.auth.dto.response.LoginResponse;
import com.dwell.domain.auth.dto.response.RefreshTokenResponse;
import com.dwell.domain.auth.exception.AuthErrorCode;
import com.dwell.domain.user.entity.RefreshToken;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.enums.Role;
import com.dwell.domain.user.repository.RefreshTokenRepository;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import com.dwell.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long FOURTEEN_DAYS_MS = 1209600000L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("로그인: Access Token, Refresh Token 발급, DB에는 원본 대신 해시 저장")
    void loginIssuesRefreshToken() {
        User user = userOf(1L);
        given(userRepository.findByEmail("test01@gmail.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("abc123", "hashed")).willReturn(true);
        stubTokenCreation("refresh-new");

        LoginResponse response = authService.login(loginRequest());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-new");
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo("refresh-new");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(13));
    }

    @Test
    @DisplayName("재발급: 사용한 토큰 폐기, 새 Access Token, Refresh Token 발급")
    void refresh() {
        RefreshToken oldToken = refreshTokenOf(LocalDateTime.now().plusDays(1));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(oldToken));
        stubTokenCreation("refresh-new");

        RefreshTokenResponse response = authService.refresh(refreshRequest("refresh-old"));

        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-new");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("재발급: 없는 토큰이면 INVALID_REFRESH_TOKEN")
    void refreshNotFound() {
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshRequest("unknown")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("재발급: 이미 폐기된 토큰이면 INVALID_REFRESH_TOKEN, 새 토큰 발급 X")
    void refreshRevoked() {
        RefreshToken revoked = refreshTokenOf(LocalDateTime.now().plusDays(1));
        revoked.revoke();
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(refreshRequest("refresh-old")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("재발급: 만료된 토큰이면 INVALID_REFRESH_TOKEN, 폐기·발급 X")
    void refreshExpired() {
        RefreshToken expired = refreshTokenOf(LocalDateTime.now().minusMinutes(1));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(refreshRequest("refresh-old")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        assertThat(expired.getRevokedAt()).isNull();
        verify(refreshTokenRepository, never()).save(any());
    }

    // ========== 픽스처 ==========

    private void stubTokenCreation(String refreshToken) {
        given(jwtProvider.createAccessToken(any())).willReturn("access");
        given(jwtProvider.createRefreshToken()).willReturn(refreshToken);
        given(jwtProvider.getRefreshTokenExpiration()).willReturn(FOURTEEN_DAYS_MS);
    }

    private User userOf(Long id) {
        User user = User.builder()
                .email("test01@gmail.com")
                .passwordHash("hashed")
                .nickname("kjm")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private RefreshToken refreshTokenOf(LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .user(userOf(1L))
                .tokenHash("hash")
                .expiresAt(expiresAt)
                .build();
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test01@gmail.com");
        ReflectionTestUtils.setField(request, "password", "abc123");
        return request;
    }

    private RefreshTokenRequest refreshRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }
}
