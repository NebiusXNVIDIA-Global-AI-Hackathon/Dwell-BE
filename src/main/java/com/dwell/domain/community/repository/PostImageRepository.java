package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
