package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
