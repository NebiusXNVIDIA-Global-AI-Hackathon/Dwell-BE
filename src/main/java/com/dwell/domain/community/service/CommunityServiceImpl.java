package com.dwell.domain.community.service;

import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.community.dto.request.PostCreateRequest;
import com.dwell.domain.community.dto.response.PostCreateResponse;
import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.entity.PostImage;
import com.dwell.domain.community.entity.PostTag;
import com.dwell.domain.community.entity.Tag;
import com.dwell.domain.community.exception.CommunityErrorCode;
import com.dwell.domain.community.repository.PostImageRepository;
import com.dwell.domain.community.repository.PostRepository;
import com.dwell.domain.community.repository.PostTagRepository;
import com.dwell.domain.community.repository.TagRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.global.common.enums.FloorBand;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final int LOWER_FLOOR_MAX = 3; // 1~3층 Lower
    private static final int MIDDLE_FLOOR_MAX = 6; // 4~6층 Middle, 7층~ Upper

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final PostImageRepository postImageRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final FileRepository fileRepository;

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
        List<File> images = findImages(userId, request.getImageFileIds());

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



    // ========== 헬퍼 함수 ==========

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

    // 이미지 파일 조회 + 검증 (요청 순서 유지)
    private List<File> findImages(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = fileIds.stream().distinct().toList();
        Map<Long, File> files = fileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));

        boolean valid = files.size() == ids.size()
                && files.values().stream().allMatch(f ->
                        f.getUser().getId().equals(userId) && f.getPurpose() == FilePurpose.POST_IMAGE)
                && !postImageRepository.existsByFileIdIn(ids);
        if (!valid) {
            log.warn("[CommunityService] 게시물 작성 서비스 - 사용할 수 없는 이미지: userId={}, fileIds={}", userId, ids);
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
