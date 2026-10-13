package com.dwell.domain.community.service;

import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostListResponse;
import com.dwell.domain.community.enums.BoardFilter;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;
import com.dwell.domain.community.dto.response.ReplyCreateResponse;

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

    /**
     * [게시물 목록·검색 서비스]
     * @author 김민호
     * @description: 내 건물 게시물을 탭(RECOMMENDED / FREE / TIPS)·제목·태그로 조회합니다.
     *               RECOMMENDED는 내 진행 중 케이스와 관련된 태그가 있는 글 -> 좋아요 많은 글 -> 최신 순이고,
     *               FREE·TIPS는 최신 순입니다. 태그로 검색하면 tagSummary(글 수, 내 케이스 관련 글 수)를 함께 줍니다.
     *
     * @request userId, board, q, tag, page, size
     * @return PostListResponse(tagSummary, content, page, size, totalElements, hasNext)
     */
    PostListResponse getPosts(Long userId, BoardFilter board, String q, String tag, int page, int size);

    /**
     * [게시물 상세 서비스]
     * @author 김민호
     * @description: 내 건물 게시물의 상세(태그·이미지·좋아요·댓글)를 조회합니다.
     *               다른 건물 게시물은 없는 게시물로 처리하고, 작성자는 층 구간(Middle Floor neighbor)으로만 보여줍니다.
     *
     * @request postId, userId
     * @return PostDetailResponse(postId, board, title, content, author, tags, images, likeCount, likedByMe, createdAt, replies)
     */
    PostDetailResponse getPost(Long postId, Long userId);

    /**
     * [댓글 작성 서비스]
     * @author 김민호
     * @description: 내 건물 게시물에 댓글을 작성합니다. 작성자 층은 Lower / Middle / Upper Floor로만 저장합니다.
     *               다른 건물 게시물이거나 집 등록이 없으면 없는 게시물로 처리합니다.
     *
     * @request postId, userId, ReplyCreateRequest(content)
     * @return ReplyCreateResponse(replyId)
     */
    ReplyCreateResponse createReply(Long postId, Long userId, ReplyCreateRequest request);

    /**
     * [좋아요 / 좋아요 취소 서비스]
     * @author 김민호
     * @description: 내 건물 게시물에 좋아요를 누르거나 취소합니다. 같은 값을 여러 번 보내도 결과가 같습니다(멱등).
     *               다른 건물 게시물은 없는 게시물로 처리합니다.
     *
     * @request postId, userId, PostLikeRequest(liked)
     * @return PostLikeResponse(liked, likeCount)
     */
    PostLikeResponse likePost(Long postId, Long userId, PostLikeRequest request);
}
