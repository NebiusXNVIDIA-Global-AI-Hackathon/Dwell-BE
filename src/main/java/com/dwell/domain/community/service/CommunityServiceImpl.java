package com.dwell.domain.community.service;

import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.PostUpdateRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.dto.response.PostAuthorResponse;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostImageResponse;
import com.dwell.domain.community.dto.response.PostListResponse;
import com.dwell.domain.community.dto.response.PostSummaryResponse;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.PostUpdateResponse;
import com.dwell.domain.community.dto.response.ReplyAuthorResponse;
import com.dwell.domain.community.dto.response.ReplyCreateResponse;
import com.dwell.domain.community.dto.response.ReplyResponse;
import com.dwell.domain.community.dto.response.TagCountResponse;
import com.dwell.domain.community.dto.response.TagSuggestionResponse;
import com.dwell.domain.community.dto.response.TitleSuggestionResponse;
import com.dwell.domain.community.dto.response.TagSummaryResponse;
import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostImage;
import com.dwell.domain.community.entity.PostTag;
import com.dwell.domain.community.entity.Reply;
import com.dwell.domain.community.entity.Tag;
import com.dwell.domain.community.enums.Board;
import com.dwell.domain.community.enums.BoardFilter;
import com.dwell.domain.community.exception.CommunityErrorCode;
import com.dwell.domain.community.repository.PostImageRepository;
import com.dwell.domain.community.repository.PostLikeRepository;
import com.dwell.domain.community.repository.PostRepository;
import com.dwell.domain.community.repository.PostTagRepository;
import com.dwell.domain.community.repository.ReplyRepository;
import com.dwell.domain.community.repository.TagRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.enums.FloorBand;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private static final int MAX_TAGS = 5;
    private static final String DELETED_USER = "Deleted user";
    private static final String DEFAULT_NICKNAME = "Neighbor"; // 닉네임 미설정 사용자
    private static final int PREVIEW_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TAG_SUGGESTIONS = 10;

    // 케이스 이슈 카테고리 -> 관련 태그 (추천 글·"related to your case" 판단용, 태그는 소문자·# 없이 저장됨)
    private static final Map<IssueCategory, List<String>> ISSUE_TAG_KEYWORDS = Map.of(
            IssueCategory.HEAT_HOT_WATER, List.of("heat", "noheat", "hotwater", "nohotwater", "radiator", "boiler"),
            IssueCategory.WATER_FLOODING, List.of("waterleak", "leak", "ceilingleak", "flood", "flooding", "water"),
            IssueCategory.PLUMBING, List.of("plumbing", "pipe", "pipeburst", "waterpipe", "clog", "toilet", "sink"),
            IssueCategory.MOLD_MOISTURE, List.of("mold", "moisture", "damp"),
            IssueCategory.PESTS, List.of("pests", "roaches", "cockroach", "mice", "rats", "bedbugs"),
            IssueCategory.ELECTRICITY, List.of("electricity", "power", "outage", "outlet"),
            IssueCategory.GAS, List.of("gas", "gasleak"),
            IssueCategory.APPLIANCES, List.of("appliance", "fridge", "stove", "oven"),
            IssueCategory.VENTILATION, List.of("ventilation", "fan", "airflow"),
            IssueCategory.SAFETY, List.of("safety", "security", "lock", "smokedetector"));
    private static final int LOWER_FLOOR_MAX = 3; // 1~3층 Lower
    private static final int MIDDLE_FLOOR_MAX = 6; // 4~6층 Middle, 7층~ Upper

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final ReplyRepository replyRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final S3Uploader s3Uploader;
    private final CaseRepository caseRepository;

    // 게시물 작성 서비스
    @Override
    @Transactional
    public PostCreateResponse createPost(Long userId, PostCreateRequest request) {

        log.info("[CommunityService] 게시물 작성 서비스 - 시작: userId={}, board={}", userId, request.getBoard());

        // 내 집 조회 (건물 게시판에 쓰기 때문에 주소 등록 필수)
        UserPlace place = userPlaceRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[CommunityService] 게시물 작성 서비스 - 등록된 집이 없습니다: userId={}", userId);
                    return new CustomException(CommunityErrorCode.PLACE_NOT_REGISTERED);
                });

        // 태그 정리 (소문자, 공백·# 제거, 중복 제거)
        List<String> tagNames = normalizeTags(request.getTags());
        if (tagNames.size() > MAX_TAGS) {
            log.warn("[CommunityService] 게시물 작성 서비스 - 태그가 너무 많습니다: count={}", tagNames.size());
            throw new CustomException(CommunityErrorCode.TOO_MANY_TAGS);
        }

        // 이미지 확인 (내가 올린 게시물용 이미지, 다른 글에 쓰이지 않은 것)
        List<File> images = findImages(userId, request.getImageFileIds(), null);

        // 게시물 저장
        Post post = postRepository.save(Post.builder()
                .building(place.getBuilding())
                .user(place.getUser())
                .board(request.getBoard())
                .title(request.getTitle().strip())
                .content(request.getContent().strip())
                .floorBand(toFloorBand(place.getFloor()))
                .build());

        // 태그 연결 (없는 태그는 새로 생성)
        for (Tag tag : findOrCreateTags(tagNames)) {
            postTagRepository.save(PostTag.builder().post(post).tag(tag).build());
        }

        // 이미지 연결 (요청 순서대로)
        for (int i = 0; i < images.size(); i++) {
            postImageRepository.save(PostImage.builder().post(post).file(images.get(i)).sortOrder(i).build());
        }

        log.info("[CommunityService] 게시물 작성 서비스 - 완료: postId={}, tags={}, images={}", post.getId(), tagNames.size(), images.size());

        return PostCreateResponse.builder()
                .postId(post.getId())
                .build();
    }

    // 게시물 삭제 서비스
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {

        log.info("[CommunityService] 게시물 삭제 서비스 - 시작: postId={}, userId={}", postId, userId);

        // 내 게시물 조회
        Post post = findOwnedPost(postId, userId);

        // 첨부 이미지 파일 조회 (파일은 DB에서 자동으로 지워지지 않음)
        List<File> files = postImageRepository.findFilesByPost(post);

        // 게시물 삭제 (댓글·이미지 연결·태그 연결·좋아요는 DB의 ON DELETE CASCADE로 함께 삭제)
        postRepository.delete(post);

        // 파일 삭제 (post_image가 file을 참조하므로 게시물 삭제 뒤에, S3 원본은 커밋 뒤에)
        fileService.deleteFiles(files);

        log.info("[CommunityService] 게시물 삭제 서비스 - 완료: postId={}, files={}", postId, files.size());
    }

    // 게시물 수정 서비스
    @Override
    @Transactional
    public PostUpdateResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {

        log.info("[CommunityService] 게시물 수정 서비스 - 시작: postId={}, userId={}", postId, userId);

        // 내 게시물 조회
        Post post = findOwnedPost(postId, userId);

        // 제목·내용·게시판 수정 (보낸 값만)
        post.update(request.getBoard(),
                request.getTitle() == null ? null : request.getTitle().strip(),
                request.getContent() == null ? null : request.getContent().strip());

        // 태그 교체 (보냈을 때만)
        if (request.getTags() != null) {
            List<String> tagNames = normalizeTags(request.getTags());
            if (tagNames.size() > MAX_TAGS) {
                log.warn("[CommunityService] 게시물 수정 서비스 - 태그가 너무 많습니다: count={}", tagNames.size());
                throw new CustomException(CommunityErrorCode.TOO_MANY_TAGS);
            }
            postTagRepository.deleteAllByPost(post);
            for (Tag tag : findOrCreateTags(tagNames)) {
                postTagRepository.save(PostTag.builder().post(post).tag(tag).build());
            }
        }

        // 이미지 교체 (보냈을 때만, 요청 순서대로)
        if (request.getImageFileIds() != null) {
            List<File> images = findImages(userId, request.getImageFileIds(), post);

            // 빠진 이미지 파일 (연결을 지운 뒤 파일 삭제)
            List<File> removed = postImageRepository.findFilesByPost(post).stream()
                    .filter(f -> !images.contains(f))
                    .toList();

            postImageRepository.deleteAllByPost(post);
            for (int i = 0; i < images.size(); i++) {
                postImageRepository.save(PostImage.builder().post(post).file(images.get(i)).sortOrder(i).build());
            }
            fileService.deleteFiles(removed);
        }

        log.info("[CommunityService] 게시물 수정 서비스 - 완료: postId={}", postId);

        return PostUpdateResponse.builder()
                .postId(post.getId())
                .build();
    }

    // 게시물 목록·검색 서비스
    @Override
    public PostListResponse getPosts(Long userId, BoardFilter board, String q, String tag, int page, int size) {

        log.info("[CommunityService] 게시물 목록 서비스 - 시작: userId={}, board={}, q={}, tag={}, page={}, size={}",
                userId, board, q, tag, page, size);

        // 내 집 조회 (게시판은 건물 단위)
        UserPlace place = userPlaceRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[CommunityService] 게시물 목록 서비스 - 등록된 집이 없습니다: userId={}", userId);
                    return new CustomException(CommunityErrorCode.PLACE_NOT_REGISTERED);
                });
        Long buildingId = place.getBuilding().getId();

        // 검색어·태그 정리 (빈 값이면 필터 없음, 태그는 "#Ceiling Leak" -> "ceilingleak")
        String title = q == null || q.isBlank() ? null : q.strip();
        String tagName = normalizeTags(tag == null ? null : List.of(tag)).stream().findFirst().orElse(null);
        String boardName = board == BoardFilter.RECOMMENDED ? null : board.name();

        // 내 진행 중 케이스와 관련된 태그 (추천 정렬·태그 요약용, 빈 목록이면 IN ()이 깨져서 빈 값 하나)
        List<String> keywords = caseRepository.findActiveIssueCategoriesByUserId(userId).stream()
                .flatMap(category -> ISSUE_TAG_KEYWORDS.getOrDefault(category, List.of()).stream())
                .distinct()
                .toList();
        List<String> keywordParam = keywords.isEmpty() ? List.of("") : keywords;

        // 게시물 ID 페이지 조회
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<Long> ids = postRepository.findIdsInBuilding(buildingId, boardName, title, tagName,
                board == BoardFilter.RECOMMENDED, keywordParam, pageRequest);

        // 태그 검색 요약 (태그로 검색했을 때만)
        TagSummaryResponse tagSummary = tagName == null ? null : TagSummaryResponse.builder()
                .tag("#" + tagName)
                .postCount(ids.getTotalElements())
                .relatedToMyCase(keywords.isEmpty() ? 0 : postRepository.countRelatedInBuildingByTag(buildingId, tagName, keywords))
                .build();

        log.info("[CommunityService] 게시물 목록 서비스 - 완료: userId={}, total={}, returned={}",
                userId, ids.getTotalElements(), ids.getNumberOfElements());

        return PostListResponse.builder()
                .tagSummary(tagSummary)
                .content(toSummaries(ids.getContent()))
                .page(ids.getNumber())
                .size(ids.getSize())
                .totalElements(ids.getTotalElements())
                .hasNext(ids.hasNext())
                .build();
    }

    // 태그 자동완성 서비스
    @Override
    public TagSuggestionResponse suggestTags(Long userId, String q, boolean withTitles) {

        log.info("[CommunityService] 태그 자동완성 서비스 - 시작: userId={}, q={}, withTitles={}", userId, q, withTitles);

        // 내 집 조회 (게시판은 건물 단위)
        UserPlace place = userPlaceRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[CommunityService] 태그 자동완성 서비스 - 등록된 집이 없습니다: userId={}", userId);
                    return new CustomException(CommunityErrorCode.PLACE_NOT_REGISTERED);
                });
        Long buildingId = place.getBuilding().getId();

        // 입력 정리 ("#Ceil" -> "ceil", 비어 있으면 건물에서 많이 쓰인 태그)
        String tagName = normalizeTags(q == null ? null : List.of(q)).stream().findFirst().orElse("");

        // 태그 추천 (입력과 같은 태그 먼저 -> 글 많은 순)
        List<TagCountResponse> items = postTagRepository.findTagCountsInBuilding(buildingId, escapeLike(tagName), tagName,
                        PageRequest.of(0, MAX_TAG_SUGGESTIONS)).stream()
                .map(row -> TagCountResponse.builder()
                        .tag("#" + row[0])
                        .postCount((Long) row[1])
                        .build())
                .toList();
        boolean exactMatch = !tagName.isEmpty() && !items.isEmpty() && items.get(0).getTag().equals("#" + tagName);

        // 제목 추천 (검색창에서만, 입력이 없으면 빈 목록)
        List<TitleSuggestionResponse> titles = null;
        if (withTitles) {
            String title = q == null ? "" : q.replace("#", "").strip();
            titles = title.isEmpty() ? List.of()
                    : postRepository.findTop5ByBuildingIdAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(buildingId, title)
                    .stream()
                    .map(post -> TitleSuggestionResponse.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .board(post.getBoard())
                            .floorBand(post.getFloorBand())
                            .build())
                    .toList();
        }

        log.info("[CommunityService] 태그 자동완성 서비스 - 완료: userId={}, tags={}, exactMatch={}", userId, items.size(), exactMatch);

        return TagSuggestionResponse.builder()
                .items(items)
                .exactMatch(exactMatch)
                .titles(titles)
                .build();
    }

    // 게시물 상세 서비스
    @Override
    public PostDetailResponse getPost(Long postId, Long userId) {

        log.info("[CommunityService] 게시물 상세 서비스 - 시작: postId={}, userId={}", postId, userId);

        // 내 건물 게시물 조회
        Post post = findPostInMyBuilding(postId, userId);

        // 태그 (#붙여서)
        List<String> tags = postTagRepository.findTagNamesByPost(post).stream()
                .map(name -> "#" + name)
                .toList();

        // 이미지 (등록 순서대로, 비공개 버킷이라 presigned URL)
        List<PostImageResponse> images = postImageRepository.findFilesByPostOrderBySortOrder(post).stream()
                .map(file -> PostImageResponse.builder()
                        .fileId(file.getId())
                        .url(s3Uploader.getPresignedUrl(file.getStorageKey()))
                        .build())
                .toList();

        // 댓글 (오래된 순)
        List<ReplyResponse> replies = replyRepository.findAllByPostWithUser(post).stream()
                .map(reply -> ReplyResponse.builder()
                        .replyId(reply.getId())
                        .author(toReplyAuthor(reply, userId))
                        .content(reply.getContent())
                        .createdAt(reply.getCreatedAt())
                        .build())
                .toList();

        // 작성자 (층 구간만, 탈퇴한 작성자는 Deleted user)
        PostAuthorResponse author = PostAuthorResponse.builder()
                .label(post.getUser() == null ? DELETED_USER : toFloorLabel(post.getFloorBand()) + " neighbor")
                .isMe(post.getUser() != null && post.getUser().getId().equals(userId))
                .build();

        log.info("[CommunityService] 게시물 상세 서비스 - 완료: postId={}, images={}, replies={}", postId, images.size(), replies.size());

        return PostDetailResponse.builder()
                .postId(post.getId())
                .board(post.getBoard())
                .title(post.getTitle())
                .content(post.getContent())
                .author(author)
                .tags(tags)
                .images(images)
                .likeCount(postLikeRepository.countByIdPostId(postId))
                .likedByMe(postLikeRepository.existsByIdPostIdAndIdUserId(postId, userId))
                .createdAt(post.getCreatedAt())
                .replies(replies)
                .build();
    }

    // 댓글 작성 서비스
    @Override
    @Transactional
    public ReplyCreateResponse createReply(Long postId, Long userId, ReplyCreateRequest request) {

        log.info("[CommunityService] 댓글 작성 서비스 - 시작: postId={}, userId={}", postId, userId);

        // 내 집 + 내 건물 게시물 조회 (댓글 작성자 층 구간에 집 정보가 필요)
        UserPlace place = userPlaceRepository.findByUserId(userId).orElse(null);
        Post post = findPostInBuilding(postId, userId, place);

        // 댓글 저장 (층은 Lower / Middle / Upper Floor로만)
        Reply reply = replyRepository.save(Reply.builder()
                .post(post)
                .user(place.getUser())
                .content(request.getContent().strip())
                .floorBand(toFloorBand(place.getFloor()))
                .build());

        log.info("[CommunityService] 댓글 작성 서비스 - 완료: postId={}, replyId={}", postId, reply.getId());

        return ReplyCreateResponse.builder()
                .replyId(reply.getId())
                .build();
    }

    // 좋아요 / 좋아요 취소 서비스
    @Override
    @Transactional
    public PostLikeResponse likePost(Long postId, Long userId, PostLikeRequest request) {

        log.info("[CommunityService] 좋아요 서비스 - 시작: postId={}, userId={}, liked={}", postId, userId, request.getLiked());

        // 내 건물 게시물 조회 (게시물 존재 여부만 확인)
        findPostInMyBuilding(postId, userId);

        // 좋아요 추가 / 취소 (이미 그 상태면 아무 일 없음)
        if (request.getLiked()) {
            postLikeRepository.insertIgnore(postId, userId);
        } else {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        }

        long likeCount = postLikeRepository.countByIdPostId(postId);

        log.info("[CommunityService] 좋아요 서비스 - 완료: postId={}, liked={}, likeCount={}", postId, request.getLiked(), likeCount);

        return PostLikeResponse.builder()
                .liked(request.getLiked())
                .likeCount(likeCount)
                .build();
    }



    // ========== 헬퍼 함수 ==========

    // 내 게시물 조회 (없으면 404, 남의 글이면 403 / 탈퇴한 작성자의 글은 user가 NULL)
    private Post findOwnedPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("[CommunityService] 게시물 조회 실패 - 존재하지 않는 게시물: postId={}", postId);
                    return new CustomException(CommunityErrorCode.POST_NOT_FOUND);
                });

        if (post.getUser() == null || !post.getUser().getId().equals(userId)) {
            log.warn("[CommunityService] 게시물 조회 실패 - 타인 게시물 접근 시도: postId={}, userId={}", postId, userId);
            throw new CustomException(CommunityErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    // 게시물 ID -> 목록 카드 (태그·썸네일·좋아요 수·댓글 수는 한 번에 모아서 조회, ID 순서 유지)
    private List<PostSummaryResponse> toSummaries(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> posts = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        Map<Long, List<String>> tags = postTagRepository.findTagNamesByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0],
                        Collectors.mapping(row -> "#" + row[1], Collectors.toList())));
        Map<Long, String> thumbnailKeys = toMap(postImageRepository.findThumbnailKeysByPostIds(postIds));
        Map<Long, Long> likeCounts = toMap(postLikeRepository.countByPostIds(postIds));
        Map<Long, Long> replyCounts = toMap(replyRepository.countByPostIds(postIds));

        return postIds.stream()
                .map(posts::get)
                .map(post -> PostSummaryResponse.builder()
                        .postId(post.getId())
                        .board(post.getBoard())
                        .isTip(post.getBoard() == Board.TIPS)
                        .floorBand(post.getFloorBand())
                        .title(post.getTitle())
                        .preview(toPreview(post.getContent()))
                        .tags(tags.getOrDefault(post.getId(), List.of()))
                        .thumbnailUrl(thumbnailKeys.containsKey(post.getId())
                                ? s3Uploader.getPresignedUrl(thumbnailKeys.get(post.getId())) : null)
                        .likeCount(likeCounts.getOrDefault(post.getId(), 0L))
                        .replyCount(replyCounts.getOrDefault(post.getId(), 0L))
                        .createdAt(post.getCreatedAt())
                        .build())
                .toList();
    }

    // [postId, 값] 행 목록 -> Map
    @SuppressWarnings("unchecked")
    private <V> Map<Long, V> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (V) row[1]));
    }

    // LIKE 특수문자 이스케이프 (태그는 공백·#만 지워서 %, _가 남을 수 있음)
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // 내용 미리보기 (줄바꿈은 공백으로, 100자 넘으면 …)
    private String toPreview(String content) {
        String oneLine = content.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH).strip() + "…";
    }

    // 내 건물 게시물 조회 (게시판은 건물 단위라 다른 건물 글·집 미등록이면 없는 글로 처리)
    private Post findPostInMyBuilding(Long postId, Long userId) {
        return findPostInBuilding(postId, userId, userPlaceRepository.findByUserId(userId).orElse(null));
    }

    // 게시물 조회 + 내 집(place)과 같은 건물인지 확인 (place가 null이면 집 미등록)
    private Post findPostInBuilding(Long postId, Long userId, UserPlace place) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("[CommunityService] 게시물 조회 실패 - 존재하지 않는 게시물: postId={}", postId);
                    return new CustomException(CommunityErrorCode.POST_NOT_FOUND);
                });

        if (place == null || !place.getBuilding().getId().equals(post.getBuilding().getId())) {
            log.warn("[CommunityService] 게시물 조회 실패 - 다른 건물 게시물 접근 시도: postId={}, userId={}", postId, userId);
            throw new CustomException(CommunityErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    // 댓글 작성자 (닉네임 + 이니셜, 탈퇴한 작성자는 Deleted user)
    private ReplyAuthorResponse toReplyAuthor(Reply reply, Long userId) {
        User user = reply.getUser();
        String nickname = user == null ? DELETED_USER
                : user.getNickname() != null ? user.getNickname() : DEFAULT_NICKNAME;
        return ReplyAuthorResponse.builder()
                .nickname(nickname)
                .initials(user == null ? null : toInitials(nickname))
                .floorBand(reply.getFloorBand())
                .isMe(user != null && user.getId().equals(userId))
                .build();
    }

    // 닉네임 이니셜 (GuiltyMo -> GM, LeftMino -> LM, DORAI -> DI, mino -> MO)
    private String toInitials(String nickname) {
        String capitals = nickname.chars()
                .filter(Character::isUpperCase)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (capitals.length() >= 2 && capitals.length() < nickname.length()) {
            return capitals.substring(0, 2);
        }
        if (nickname.length() == 1) {
            return nickname.toUpperCase();
        }
        return (nickname.substring(0, 1) + nickname.substring(nickname.length() - 1)).toUpperCase();
    }

    // 층 구간 -> 표시 문구 (MIDDLE -> Middle Floor)
    private String toFloorLabel(FloorBand floorBand) {
        String name = floorBand.name();
        return name.charAt(0) + name.substring(1).toLowerCase() + " Floor";
    }

    // 태그 정리 ("#Pipe Burst" -> "pipeburst"), 빈 값·중복 제거
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(t -> t != null)
                .map(t -> t.replaceAll("[\\s#]", "").toLowerCase())
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }

    // 이름으로 태그 조회, 없으면 생성
    private List<Tag> findOrCreateTags(List<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, Tag> existing = tagRepository.findByNameIn(names).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> tags = new ArrayList<>();
        for (String name : names) {
            Tag tag = existing.get(name);
            tags.add(tag != null ? tag : tagRepository.save(Tag.builder().name(name).build()));
        }
        return tags;
    }

    // 이미지 파일 조회 + 검증 (요청 순서 유지, 수정 중인 게시물에 이미 붙은 이미지는 허용)
    private List<File> findImages(Long userId, List<Long> fileIds, Post editingPost) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = fileIds.stream().distinct().toList();
        Map<Long, File> files = fileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));

        boolean valid = files.size() == ids.size()
                && files.values().stream().allMatch(f ->
                        f.getUser().getId().equals(userId) && f.getPurpose() == FilePurpose.POST_IMAGE)
                && !(editingPost == null
                        ? postImageRepository.existsByFileIdIn(ids)
                        : postImageRepository.existsByFileIdInAndPostNot(ids, editingPost));
        if (!valid) {
            log.warn("[CommunityService] 사용할 수 없는 이미지: userId={}, fileIds={}", userId, ids);
            throw new CustomException(CommunityErrorCode.INVALID_POST_IMAGE);
        }
        return ids.stream().map(files::get).toList();
    }

    // 층 -> 층 구간 (정확한 층은 노출하지 않음, 층 정보 없으면 Middle)
    private FloorBand toFloorBand(Integer floor) {
        if (floor == null) {
            return FloorBand.MIDDLE;
        }
        if (floor <= LOWER_FLOOR_MAX) {
            return FloorBand.LOWER;
        }
        return floor <= MIDDLE_FLOOR_MAX ? FloorBand.MIDDLE : FloorBand.UPPER;
    }
}
