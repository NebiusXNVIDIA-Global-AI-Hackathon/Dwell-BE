package com.dwell.domain.community.service;

import com.dwell.domain.community.dto.request.LandlordPostSearchRequest;
import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.LandlordPostListResponse;
import com.dwell.domain.community.dto.response.LandlordPostSummaryResponse;
import com.dwell.domain.community.dto.response.MyPostListResponse;
import com.dwell.domain.community.dto.response.MyReplyListResponse;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.PostListResponse;
import com.dwell.domain.community.dto.response.PostSummaryResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;
import com.dwell.domain.community.dto.response.ReplyCreateResponse;
import com.dwell.domain.community.dto.response.TagSuggestionResponse;
import com.dwell.domain.community.enums.BoardFilter;
import com.dwell.global.common.enums.IssueCategory;

import java.util.List;

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
     * [내 게시물 목록 서비스]
     * @author 김정모
     * @description: My Activities Posts 탭. 지금 내 건물에 쓴 게시물을 최신 순으로 줍니다 (주소를 바꾸면 이전 건물 글은 제외).
     *               탭 숫자(내 게시물 수, 내 댓글 수)를 함께 줍니다.
     *
     * @request userId, page, size
     * @return MyPostListResponse(counts, content, page, size, totalElements, hasNext)
     */
    MyPostListResponse getMyPosts(Long userId, int page, int size);

    /**
     * [내 댓글 목록 서비스]
     * @author 김정모
     * @description: My Activities Replies 탭. 지금 내 건물 게시물에 단 댓글을 최신 순으로, 게시물 제목과 함께 줍니다.
     *
     * @request userId, page, size
     * @return MyReplyListResponse(content, page, size, totalElements, hasNext)
     */
    MyReplyListResponse getMyReplies(Long userId, int page, int size);

    /**
     * [태그 자동완성 서비스]
     * @author 김민호
     * @description: 내 건물 게시물에 달린 태그 중 입력으로 시작하는 태그를 추천합니다 (입력과 같은 태그 먼저, 나머지는 글 많은 순, 최대 10개).
     *               입력과 같은 태그가 없으면 exactMatch=false (클라가 + Create new tag 노출, 실제 생성은 게시물 작성 시).
     *               withTitles=true(검색창)면 제목에 입력이 들어간 글도 최신 순 5개 함께 줍니다.
     *
     * @request userId, q, withTitles
     * @return TagSuggestionResponse(items, exactMatch, titles)
     */
    TagSuggestionResponse suggestTags(Long userId, String q, boolean withTitles);

    /**
     * [이슈 관련 글 서비스]
     * @author 김민호
     * @description: 건물 게시물 중 이슈 카테고리 관련 태그가 달린 글을 좋아요 많은 순으로 5개 줍니다 (My Place Issue Details용).
     *
     * @request buildingId, category
     * @return List<PostSummaryResponse>
     */
    List<PostSummaryResponse> getIssuePosts(Long buildingId, IssueCategory category);

    /**
     * [집주인 언급 글 요약 서비스]
     * @author 김민호
     * @description: 내 건물 게시물 중 집주인을 언급한 글 수, 분류별 수, 미리보기 3개를 줍니다 (Building Insights · Tenants 탭).
     *               집주인 언급·분류는 아직 제목·내용 키워드로 판단합니다. 3개 미만이면 NOT_ENOUGH_DATA입니다.
     *
     * @request userId
     * @return LandlordPostSummaryResponse(status, postCount, minimumRequired, topicCounts, previewPosts)
     */
    LandlordPostSummaryResponse getLandlordPostSummary(Long userId);

    /**
     * [집주인 언급 글 목록 서비스]
     * @author 김민호
     * @description: 내 건물 집주인 언급 글을 검색·필터(분류·이슈·층·기간)·정렬해서 페이지로 줍니다 (Posts about landlord).
     *               칩 숫자(topicCounts)는 분류 외 필터를 적용한 기준입니다.
     *
     * @request userId, LandlordPostSearchRequest(q, topic, issueType, floorBand, posted, sort, page, size)
     * @return LandlordPostListResponse(address, topicCounts, content, page, size, totalElements, hasNext)
     */
    LandlordPostListResponse getLandlordPosts(Long userId, LandlordPostSearchRequest request);

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
