package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostTag;
import com.dwell.domain.community.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {

    // 게시물 태그 연결 전부 삭제 (바로 실행 -> 같은 태그를 다시 연결해도 PK에 안 걸림)
    @Modifying
    @Query("delete from PostTag pt where pt.post = :post")
    void deleteAllByPost(Post post);
}
