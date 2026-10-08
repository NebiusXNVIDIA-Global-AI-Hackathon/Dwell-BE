package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.PostTag;
import com.dwell.domain.community.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
}
