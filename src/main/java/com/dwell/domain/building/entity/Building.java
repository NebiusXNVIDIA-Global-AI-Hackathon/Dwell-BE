package com.dwell.domain.building.entity;

import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Building extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NYC Borough-Block-Lot, the key for joining HPD data
    @Column(nullable = false, unique = true, length = 10)
    private String bbl;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(nullable = false, length = 10)
    private String zip;

    private LocalDateTime hpdSyncedAt;

    @Builder
    public Building(String bbl, String street, String city, String state, String zip) {
        this.bbl = bbl;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }
}
