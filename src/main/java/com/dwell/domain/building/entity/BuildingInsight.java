package com.dwell.domain.building.entity;

import com.dwell.domain.building.enums.InsightDataSource;
import com.dwell.domain.building.enums.LandlordRisk;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Batch-computed landlord response stats per building
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuildingInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false, unique = true)
    private Building building;

    private Integer avgDaysToResolve;

    // e.g. 4.2
    @Column(precision = 4, scale = 1)
    private BigDecimal avgResponseDays;

    // 0-100 percent
    private Integer noResponseRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LandlordRisk landlordRisk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InsightDataSource dataSource;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @Builder
    public BuildingInsight(Building building, Integer avgDaysToResolve, BigDecimal avgResponseDays,
                           Integer noResponseRate, LandlordRisk landlordRisk, InsightDataSource dataSource,
                           LocalDateTime computedAt) {
        this.building = building;
        this.avgDaysToResolve = avgDaysToResolve;
        this.avgResponseDays = avgResponseDays;
        this.noResponseRate = noResponseRate;
        this.landlordRisk = landlordRisk;
        this.dataSource = dataSource;
        this.computedAt = computedAt;
    }
}
