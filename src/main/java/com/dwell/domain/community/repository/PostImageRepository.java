package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostImage;
import com.dwell.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 이미 다른 게시물에 쓰인 파일인지
    boolean existsByFileIdIn(Collection<Long> fileIds);

    // 게시물 이미지 파일 (게시물 삭제 시 파일 정리용, PostImage 엔티티는 불러오지 않음)
    @Query("select pi.file from PostImage pi where pi.post = :post")
    List<File> findFilesByPost(Post post);
}
