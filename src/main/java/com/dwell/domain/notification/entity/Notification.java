package com.dwell.domain.notification.entity;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.notification.enums.NotificationType;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.LocalizedText;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Recipient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Case caseEntity;

    // For BUILDING_PATTERN
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText body;

    private LocalDateTime readAt;

    @Builder
    public Notification(User user, Case caseEntity, Building building, NotificationType type,
                        LocalizedText title, LocalizedText body) {
        this.user = user;
        this.caseEntity = caseEntity;
        this.building = building;
        this.type = type;
        this.title = title;
        this.body = body;
    }
}
