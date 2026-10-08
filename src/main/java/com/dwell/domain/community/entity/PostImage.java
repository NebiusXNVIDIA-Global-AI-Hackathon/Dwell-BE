package com.dwell.domain.community.entity;

import com.dwell.domain.file.entity.File;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // File purpose=POST_IMAGE
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private File file;

    @Column(nullable = false)
    private int sortOrder;

    @Builder
    public PostImage(Post post, File file, int sortOrder) {
        this.post = post;
        this.file = file;
        this.sortOrder = sortOrder;
    }
}
