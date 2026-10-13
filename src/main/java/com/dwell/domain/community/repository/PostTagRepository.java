package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostTag;
import com.dwell.domain.community.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {

    // 게시물 태그 이름 (태그 생성 순서)
    @Query("select t.name from PostTag pt join pt.tag t where pt.post = :post order by t.id")
    List<String> findTagNamesByPost(Post post);

    // 여러 게시물의 태그 (목록용, [postId, 태그 이름], 태그 생성 순서)
    @Query("select pt.post.id, t.name from PostTag pt join pt.tag t where pt.post.id in :postIds order by t.id")
    List<Object[]> findTagNamesByPostIds(Collection<Long> postIds);

    // 게시물 태그 연결 전부 삭제 (바로 실행 -> 같은 태그를 다시 연결해도 PK에 안 걸림)
    @Modifying
    @Query("delete from PostTag pt where pt.post = :post")
    void deleteAllByPost(Post post);
}
