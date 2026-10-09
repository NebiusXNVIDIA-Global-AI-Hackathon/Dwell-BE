package com.dwell.domain.user.entity;

import com.dwell.domain.user.enums.AuthProvider;
import com.dwell.domain.user.enums.Role;
import com.dwell.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users") // "user" is a reserved word in some DBs, so use "users"
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // NULL for Google sign-up users
    private String passwordHash;

    // en/zh/es, the language of the native mirror
    @Column(nullable = false, length = 10)
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private String profileImageKey;

    @Column(unique = true, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // Google sub
    private String providerId;

    @Builder
    public User(String email, String passwordHash, String nickname, String preferredLanguage,
                Role role, AuthProvider authProvider, String providerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        if (preferredLanguage != null) {
            this.preferredLanguage = preferredLanguage;
        }
        this.role = role;
        if (authProvider != null) {
            this.authProvider = authProvider;
        }
        this.providerId = providerId;
    }
}
