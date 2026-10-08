package com.dwell.domain.community.entity;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.community.enums.Board;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseTimeEntity;
import com.dwell.global.common.enums.FloorBand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Post on the user's building board
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    // NULL after the author deletes their account (shown as Deleted user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Board board;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    // Snapshot at write time (e.g. Middle Floor neighbor)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FloorBand floorBand;

    @Builder
    public Post(Building building, User user, Board board, String title, String content, FloorBand floorBand) {
        this.building = building;
        this.user = user;
        this.board = board;
        this.title = title;
        this.content = content;
        this.floorBand = floorBand;
    }
}
