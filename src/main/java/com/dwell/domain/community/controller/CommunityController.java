package com.dwell.domain.community.controller;

import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostListResponse;
import com.dwell.domain.community.dto.response.TagSuggestionResponse;
import com.dwell.domain.community.enums.BoardFilter;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;
import com.dwell.domain.community.dto.response.ReplyCreateResponse;
import com.dwell.domain.community.service.CommunityService;
import com.dwell.global.common.BaseResponse;
import com.dwell.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "건물 이웃 커뮤니티 API")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // 게시물 작성 컨트롤러
    @Operation(summary = "게시물 작성",
            description = "내 건물 게시판에 게시물을 작성합니다. 태그는 최대 5개이며 없는 태그는 새로 만듭니다.")
    @PostMapping("/posts")
    public ResponseEntity<BaseResponse<PostCreateResponse>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request) {

        // 게시물 작성 서비스 호출
        PostCreateResponse response = communityService.createPost(userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Post created.", response));
    }

    // 게시물 삭제 컨트롤러
    @Operation(summary = "게시물 삭제",
            description = "내 게시물을 영구 삭제합니다. 댓글·태그 연결·좋아요와 첨부 이미지(S3 원본 포함)도 함께 삭제됩니다.")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {

        // 게시물 삭제 서비스 호출
        communityService.deletePost(postId, userDetails.getUser().getId());

        // 응답 반환
        return ResponseEntity.noContent().build();
    }

    // 게시물 수정 컨트롤러
    @Operation(summary = "게시물 수정",
            description = "내 게시물에서 보낸 필드만 수정합니다. 태그·이미지는 보내면 통째로 교체되고, 빠진 이미지는 파일도 삭제됩니다.")
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<BaseResponse<PostUpdateResponse>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {

        // 게시물 수정 서비스 호출
        PostUpdateResponse response = communityService.updatePost(postId, userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Post updated.", response));
    }

    // 게시물 목록·검색 컨트롤러
    @Operation(summary = "게시물 목록·검색",
            description = "내 건물 게시물을 탭(RECOMMENDED / FREE / TIPS)·제목(q)·태그(tag, # 없이)로 조회합니다. "
                    + "RECOMMENDED는 내 진행 중 케이스 관련 글 -> 좋아요 순, FREE·TIPS는 최신 순입니다. size는 최대 50입니다.")
    @GetMapping("/posts")
    public ResponseEntity<BaseResponse<PostListResponse>> getPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "RECOMMENDED") BoardFilter board,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 게시물 목록 서비스 호출
        PostListResponse response = communityService.getPosts(userDetails.getUser().getId(), board, q, tag, page, size);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Posts retrieved.", response));
    }

    // 태그 자동완성 컨트롤러
    @Operation(summary = "태그 자동완성",
            description = "내 건물 게시물 태그 중 q로 시작하는 태그를 추천합니다 (# 없어도 됨, 최대 10개). "
                    + "exactMatch=false면 + Create new tag를 보여주세요. withTitles=true(검색창)면 제목 추천도 최대 5개 줍니다.")
    @GetMapping("/tags")
    public ResponseEntity<BaseResponse<TagSuggestionResponse>> suggestTags(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean withTitles) {

        // 태그 자동완성 서비스 호출
        TagSuggestionResponse response = communityService.suggestTags(userDetails.getUser().getId(), q, withTitles);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Tags retrieved.", response));
    }

    // 게시물 상세 컨트롤러
    @Operation(summary = "게시물 상세",
            description = "내 건물 게시물의 상세와 댓글을 조회합니다. 이미지 URL은 10분 동안 유효합니다. 다른 건물 게시물은 404입니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<BaseResponse<PostDetailResponse>> getPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {

        // 게시물 상세 서비스 호출
        PostDetailResponse response = communityService.getPost(postId, userDetails.getUser().getId());

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, "Post retrieved.", response));
    }

    // 댓글 작성 컨트롤러
    @Operation(summary = "댓글 작성",
            description = "내 건물 게시물에 댓글을 작성합니다 (1~500자). 다른 건물 게시물은 404입니다.")
    @PostMapping("/posts/{postId}/replies")
    public ResponseEntity<BaseResponse<ReplyCreateResponse>> createReply(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody ReplyCreateRequest request) {

        // 댓글 작성 서비스 호출
        ReplyCreateResponse response = communityService.createReply(postId, userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(201, "Reply created.", response));
    }

    // 좋아요 / 좋아요 취소 컨트롤러
    @Operation(summary = "좋아요 / 좋아요 취소",
            description = "liked=true면 좋아요, false면 취소합니다. 같은 값을 여러 번 보내도 결과가 같습니다(멱등).")
    @PutMapping("/posts/{postId}/like")
    public ResponseEntity<BaseResponse<PostLikeResponse>> likePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody PostLikeRequest request) {

        // 좋아요 서비스 호출
        PostLikeResponse response = communityService.likePost(postId, userDetails.getUser().getId(), request);

        // 응답 변환
        return ResponseEntity.ok(BaseResponse.success(200, request.getLiked() ? "Post liked." : "Post unliked.", response));
    }
}
