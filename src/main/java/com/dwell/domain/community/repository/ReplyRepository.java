package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.Reply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    // 게시물 댓글 (오래된 순, 작성자 함께 조회 -> 댓글마다 user 쿼리 안 나감)
    @Query("select r from Reply r left join fetch r.user where r.post = :post order by r.createdAt, r.id")
    List<Reply> findAllByPostWithUser(Post post);

    // 여러 게시물의 댓글 수 (목록용, [postId, count])
    @Query("select r.post.id, count(r) from Reply r where r.post.id in :postIds group by r.post.id")
    List<Object[]> countByPostIds(Collection<Long> postIds);

    // 내가 쓴 댓글 (지금 내 건물 글에 단 댓글, 최신 순, 게시물 제목용으로 게시물 함께 조회)
    @EntityGraph(attributePaths = "post")
    Page<Reply> findByUserIdAndPostBuildingIdOrderByCreatedAtDescIdDesc(Long userId, Long buildingId, Pageable pageable);

    // 내가 쓴 댓글 수 (지금 내 건물, My Activities 탭 숫자용)
    long countByUserIdAndPostBuildingId(Long userId, Long buildingId);
}
