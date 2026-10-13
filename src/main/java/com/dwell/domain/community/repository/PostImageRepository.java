package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 이미 다른 게시물에 쓰인 파일인지
    boolean existsByFileIdIn(Collection<Long> fileIds);
}
