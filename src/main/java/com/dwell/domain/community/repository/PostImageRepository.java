package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostImage;
import com.dwell.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 이미 다른 게시물에 쓰인 파일인지
    boolean existsByFileIdIn(Collection<Long> fileIds);

    // 이 게시물 말고 다른 게시물에 쓰인 파일인지 (게시물 수정 시 기존 이미지 유지 허용)
    boolean existsByFileIdInAndPostNot(Collection<Long> fileIds, Post post);

    // 게시물 이미지 파일 (게시물 삭제 시 파일 정리용, PostImage 엔티티는 불러오지 않음)
    @Query("select pi.file from PostImage pi where pi.post = :post")
    List<File> findFilesByPost(Post post);

    // 게시물 이미지 파일 (등록 순서대로, 게시물 상세용)
    @Query("select pi.file from PostImage pi where pi.post = :post order by pi.sortOrder")
    List<File> findFilesByPostOrderBySortOrder(Post post);

    // 여러 게시물의 첫 번째 이미지 (목록 썸네일용, [postId, storageKey])
    @Query("select pi.post.id, f.storageKey from PostImage pi join pi.file f where pi.post.id in :postIds "
            + "and pi.sortOrder = (select min(pi2.sortOrder) from PostImage pi2 where pi2.post = pi.post)")
    List<Object[]> findThumbnailKeysByPostIds(Collection<Long> postIds);

    // 게시물 이미지 연결 전부 삭제 (바로 실행 -> 같은 파일을 다시 연결해도 file_id UNIQUE에 안 걸림)
    @Modifying
    @Query("delete from PostImage pi where pi.post = :post")
    void deleteAllByPost(Post post);
}
