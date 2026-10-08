package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.PostLike;
import com.dwell.domain.community.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
}
