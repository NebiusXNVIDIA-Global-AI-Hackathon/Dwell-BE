package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    // 게시물 댓글 (오래된 순, 작성자 함께 조회 -> 댓글마다 user 쿼리 안 나감)
    @Query("select r from Reply r left join fetch r.user where r.post = :post order by r.createdAt, r.id")
    List<Reply> findAllByPostWithUser(Post post);
}
