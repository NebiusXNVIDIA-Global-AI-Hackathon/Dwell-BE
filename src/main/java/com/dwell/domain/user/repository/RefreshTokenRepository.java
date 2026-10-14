package com.dwell.domain.user.repository;

import com.dwell.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 해시로 Refresh Token 조회 (DB에는 원본 대신 해시만 저장)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
