package com.dwell.domain.user.entity;

import com.dwell.domain.user.enums.Platform;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Push notification device
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Builder
    public Device(User user, String pushToken, Platform platform) {
        this.user = user;
        this.pushToken = pushToken;
        this.platform = platform;
    }
}
