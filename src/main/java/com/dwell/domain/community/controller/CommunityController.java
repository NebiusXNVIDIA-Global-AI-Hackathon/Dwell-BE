package com.dwell.domain.community.controller;

import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
