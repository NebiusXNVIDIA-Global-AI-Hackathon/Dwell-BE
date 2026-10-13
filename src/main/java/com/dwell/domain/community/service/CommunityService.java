package com.dwell.domain.community.service;

import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;

public interface CommunityService {

    /**
     * [게시물 작성 서비스]
     * @author 김민호
     * @description: 내 건물 게시판에 게시물을 작성합니다. 없는 태그는 새로 만들고, 이미지를 연결합니다.
     *               작성자 층은 Lower / Middle / Upper Floor로만 저장합니다.
     *
     * @request userId, PostCreateRequest(board, title, content, tags, imageFileIds)
     * @return PostCreateResponse(postId)
     */
    PostCreateResponse createPost(Long userId, PostCreateRequest request);

    /**
     * [게시물 삭제 서비스]
     * @author 김민호
     * @description: 내 게시물을 영구 삭제합니다. 댓글·태그 연결·좋아요는 DB에서 함께 지워지고,
     *               첨부 이미지 파일(DB·S3)도 삭제합니다.
     *
     * @request postId, userId
     * @return void
     */
    void deletePost(Long postId, Long userId);

    /**
     * [게시물 수정 서비스]
     * @author 김민호
     * @description: 내 게시물에서 보낸 필드만 수정합니다. 태그·이미지는 보내면 통째로 교체하고,
     *               빠진 이미지 파일(DB·S3)은 삭제합니다.
     *
     * @request postId, userId, PostUpdateRequest(board, title, content, tags, imageFileIds)
     * @return PostUpdateResponse(postId)
     */
    PostUpdateResponse updatePost(Long postId, Long userId, PostUpdateRequest request);
}
