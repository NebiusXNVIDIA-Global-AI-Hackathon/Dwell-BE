package com.dwell.domain.building.entity;

import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// The user's home (one per user)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    // e.g. Apt 4B
    @Column(nullable = false, length = 20)
    private String unit;

    // Parsed from unit (Apt 4B -> 4), used for pattern detection
    private Integer floor;

    // Parsed from unit (Apt 4B -> B), used for same-vertical-line detection
    @Column(length = 10)
    private String unitLine;

    // If false, the lease is out of MVP scope
    @Column(nullable = false)
    private boolean leaseInOwnName;

    @Column(length = 100)
    private String landlordName;

    // E.164 (+12125550123)
    @Column(length = 20)
    private String landlordPhone;

    @Builder
    public UserPlace(User user, Building building, String unit, Integer floor, String unitLine,
                     boolean leaseInOwnName, String landlordName, String landlordPhone) {
        this.user = user;
        this.building = building;
        this.unit = unit;
        this.floor = floor;
        this.unitLine = unitLine;
        this.leaseInOwnName = leaseInOwnName;
        this.landlordName = landlordName;
        this.landlordPhone = landlordPhone;
    }
}
