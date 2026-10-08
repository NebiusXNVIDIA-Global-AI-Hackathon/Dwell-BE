package com.dwell.domain.community.entity;

import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored lowercase without '#'
    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Builder
    public Tag(String name) {
        this.name = name;
    }
}
