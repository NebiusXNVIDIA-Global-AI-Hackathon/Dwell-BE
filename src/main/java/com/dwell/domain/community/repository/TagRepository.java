package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // 이름으로 태그 여러 개 조회
    List<Tag> findByNameIn(Collection<String> names);
}
