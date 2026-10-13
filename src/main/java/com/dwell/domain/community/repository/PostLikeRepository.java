package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.PostLike;
import com.dwell.domain.community.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    // 좋아요 추가 (이미 있으면 무시 -> 연타·동시 요청에도 PK 충돌 없음)
    @Modifying
    @Query(value = "insert into post_like (post_id, user_id, created_at) values (:postId, :userId, now()) "
            + "on conflict (post_id, user_id) do nothing", nativeQuery = true)
    void insertIgnore(Long postId, Long userId);

    // 좋아요 취소 (없으면 아무 일 없음)
    @Modifying
    @Query("delete from PostLike pl where pl.id.postId = :postId and pl.id.userId = :userId")
    void deleteByPostIdAndUserId(Long postId, Long userId);

    // 게시물 좋아요 수
    long countByIdPostId(Long postId);
}
