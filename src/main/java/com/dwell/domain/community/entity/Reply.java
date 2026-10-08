package com.dwell.domain.community.entity;

import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.enums.FloorBand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reply extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // NULL after the author deletes their account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    // Snapshot at write time
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FloorBand floorBand;

    @Builder
    public Reply(Post post, User user, String content, FloorBand floorBand) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.floorBand = floorBand;
    }
}
