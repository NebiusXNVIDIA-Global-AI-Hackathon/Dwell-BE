package com.dwell.domain.community.controller;

import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
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
}
