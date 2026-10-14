package com.dwell.domain.community.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.ReplyResponse;
import com.dwell.domain.community.entity.Reply;
import com.dwell.domain.community.enums.Board;
import com.dwell.domain.community.enums.BoardFilter;
import com.dwell.domain.community.dto.response.MyPostListResponse;
import com.dwell.domain.community.dto.response.MyReplyListResponse;
import com.dwell.domain.community.dto.response.PostListResponse;
import com.dwell.domain.community.enums.Board;
import com.dwell.domain.community.dto.response.TagSuggestionResponse;
import com.dwell.domain.community.dto.request.LandlordPostSearchRequest;
import com.dwell.domain.community.dto.response.LandlordPostListResponse;
import com.dwell.domain.community.dto.response.LandlordPostResponse;
import com.dwell.domain.community.dto.response.LandlordPostSummaryResponse;
import com.dwell.domain.community.enums.LandlordTopic;
import com.dwell.domain.building.enums.InsightStatus;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import com.dwell.global.common.enums.IssueCategory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.exception.CommunityErrorCode;
import com.dwell.domain.community.repository.PostImageRepository;
import com.dwell.domain.community.repository.PostLikeRepository;
import com.dwell.domain.community.repository.PostRepository;
import com.dwell.domain.community.repository.PostTagRepository;
import com.dwell.domain.community.repository.ReplyRepository;
import com.dwell.domain.community.repository.TagRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.file.service.S3Uploader;
import com.dwell.global.common.enums.FloorBand;
import com.dwell.domain.user.entity.User;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PostTagRepository postTagRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private ReplyRepository replyRepository;
    @Mock
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileService fileService;
    @Mock
    private S3Uploader s3Uploader;
    @Mock
    private CaseRepository caseRepository;

    private CommunityServiceImpl communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityServiceImpl(postRepository, tagRepository, postTagRepository, postImageRepository,
                postLikeRepository, replyRepository, userPlaceRepository, fileRepository, fileService, s3Uploader, caseRepository);
    }

    @Test
    @DisplayName("없는 게시물이면 POST_NOT_FOUND")
    void deletePostNotFound() {
        given(postRepository.findById(40L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.deletePost(40L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 사람 게시물이면 NOT_POST_AUTHOR")
    void deletePostNotAuthor() {
        Post post = postOf(author(2L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> communityService.deletePost(40L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.NOT_POST_AUTHOR);
        verify(postRepository, never()).delete(any());
        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("탈퇴한 작성자의 게시물(user NULL)은 NOT_POST_AUTHOR")
    void deletePostOfDeletedUser() {
        Post post = postOf(null);
        given(postRepository.findById(40L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> communityService.deletePost(40L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.NOT_POST_AUTHOR);
        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("내 게시물이면 게시물을 먼저 지우고 첨부 이미지 파일을 삭제")
    void deletePost() {
        Post post = postOf(author(1L));
        List<File> files = List.of(mock(File.class), mock(File.class));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(postImageRepository.findFilesByPost(post)).willReturn(files);

        communityService.deletePost(40L, 1L);

        // post_image가 file을 참조하므로 게시물(→ post_image CASCADE)이 먼저 지워져야 함
        InOrder inOrder = inOrder(postRepository, fileService);
        inOrder.verify(postRepository).delete(post);
        inOrder.verify(fileService).deleteFiles(files);
    }

    @Test
    @DisplayName("좋아요: 없는 게시물이면 POST_NOT_FOUND")
    void likePostNotFound() {
        given(postRepository.findById(40L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.likePost(40L, 1L, likeRequest(true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("좋아요: 다른 건물 게시물이면 POST_NOT_FOUND")
    void likePostOtherBuilding() {
        Post post = postIn(building(10L));
        UserPlace place = placeIn(building(20L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));

        assertThatThrownBy(() -> communityService.likePost(40L, 1L, likeRequest(true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("좋아요: 집 등록이 없으면 POST_NOT_FOUND")
    void likePostWithoutPlace() {
        Post post = mock(Post.class);
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.likePost(40L, 1L, likeRequest(true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("좋아요: liked=true면 추가(중복 무시)하고 좋아요 수 반환")
    void likePost() {
        Post post = postIn(building(10L));
        UserPlace place = placeIn(building(10L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(postLikeRepository.countByIdPostId(40L)).willReturn(9L);

        PostLikeResponse response = communityService.likePost(40L, 1L, likeRequest(true));

        verify(postLikeRepository).insertIgnore(40L, 1L);
        verify(postLikeRepository, never()).deleteByPostIdAndUserId(any(), any());
        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(9L);
    }

    @Test
    @DisplayName("좋아요 취소: liked=false면 삭제하고 좋아요 수 반환")
    void unlikePost() {
        Post post = postIn(building(10L));
        UserPlace place = placeIn(building(10L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(postLikeRepository.countByIdPostId(40L)).willReturn(8L);

        PostLikeResponse response = communityService.likePost(40L, 1L, likeRequest(false));

        verify(postLikeRepository).deleteByPostIdAndUserId(40L, 1L);
        verify(postLikeRepository, never()).insertIgnore(any(), any());
        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikeCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("목록: 집 등록이 없으면 PLACE_NOT_REGISTERED")
    void getPostsWithoutPlace() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getPosts(1L, BoardFilter.RECOMMENDED, null, null, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("목록: 케이스가 없으면 키워드는 빈 값 하나, 태그는 정리해서 검색, size는 50으로 제한")
    void getPostsFilters() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(caseRepository.findActiveIssueCategoriesByUserId(1L)).willReturn(List.of());
        given(postRepository.findIdsInBuilding(eq(10L), eq("FREE"), eq("leak"), eq("ceilingleak"), eq(false),
                eq(List.of("")), eq(PageRequest.of(0, 50))))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        PostListResponse response = communityService.getPosts(1L, BoardFilter.FREE, "  leak ", "#Ceiling Leak", -1, 500);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTagSummary().getTag()).isEqualTo("#ceilingleak");
        assertThat(response.getTagSummary().getRelatedToMyCase()).isZero();
        verify(postRepository, never()).countRelatedInBuildingByTag(any(), any(), any());
    }

    @Test
    @DisplayName("목록: RECOMMENDED는 게시판 필터 없이 추천 정렬, 진행 중 케이스 카테고리의 태그를 키워드로 사용")
    void getPostsRecommended() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(caseRepository.findActiveIssueCategoriesByUserId(1L)).willReturn(List.of(IssueCategory.PESTS));
        given(postRepository.findIdsInBuilding(eq(10L), eq(null), eq(null), eq(null), eq(true), any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PostListResponse response = communityService.getPosts(1L, BoardFilter.RECOMMENDED, " ", null, 0, 20);

        assertThat(response.getTagSummary()).isNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keywords = ArgumentCaptor.forClass(List.class);
        verify(postRepository).findIdsInBuilding(any(), any(), any(), any(), anyBoolean(), keywords.capture(), any());
        assertThat(keywords.getValue()).contains("roaches", "pests");
    }

    @Test
    @DisplayName("태그 자동완성: 집 등록이 없으면 PLACE_NOT_REGISTERED")
    void suggestTagsWithoutPlace() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.suggestTags(1L, "ceil", false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(postTagRepository);
    }

    @Test
    @DisplayName("태그 자동완성: 입력을 정리하고 LIKE 특수문자를 이스케이프, 첫 태그가 입력과 같으면 exactMatch")
    void suggestTags() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(postTagRepository.findTagCountsInBuilding(10L, "water\\_", "water_", PageRequest.of(0, 10)))
                .willReturn(List.<Object[]>of(new Object[]{"water_", 2L}, new Object[]{"water_pipe", 1L}));

        TagSuggestionResponse response = communityService.suggestTags(1L, "#Water_", false);

        assertThat(response.getItems()).extracting("tag").containsExactly("#water_", "#water_pipe");
        assertThat(response.isExactMatch()).isTrue();
        assertThat(response.getTitles()).isNull();
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("태그 자동완성: withTitles면 # 뺀 입력으로 제목 추천, 같은 태그가 없으면 exactMatch=false")
    void suggestTagsWithTitles() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        given(postTagRepository.findTagCountsInBuilding(any(), any(), any(), any()))
                .willReturn(List.<Object[]>of(new Object[]{"ceilingleak", 24L}));
        given(postRepository.findTop5ByBuildingIdAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(10L, "ceil"))
                .willReturn(List.of());

        TagSuggestionResponse response = communityService.suggestTags(1L, "#ceil", true);

        assertThat(response.isExactMatch()).isFalse();
        assertThat(response.getTitles()).isEmpty();
    }

    @Test
    @DisplayName("집주인 언급 글 요약: 3개 미만이면 NOT_ENOUGH_DATA, 분류·미리보기 없음")
    void landlordSummaryNotEnough() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Post post = landlordPost(1L, "Landlord took 3 weeks", "slow", 1);
        given(postRepository.findMentionsInBuilding(eq(10L), anyString())).willReturn(List.of(post));

        LandlordPostSummaryResponse response = communityService.getLandlordPostSummary(1L);

        assertThat(response.getStatus()).isEqualTo(InsightStatus.NOT_ENOUGH_DATA);
        assertThat(response.getPostCount()).isEqualTo(1);
        assertThat(response.getTopicCounts()).isNull();
        assertThat(response.getPreviewPosts()).isNull();
    }

    @Test
    @DisplayName("집주인 언급 글 목록: 키워드로 분류 (무응답 -> 빨리 고침 -> 느린 수리), 분류 필터는 칩 숫자에 영향 없음")
    void landlordPostsClassification() {
        Building b = building(10L);
        given(b.getStreet()).willReturn("312 E 21st St");
        UserPlace place = placeIn(b);
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Post noReply = landlordPost(1L, "How I got my landlord to reply in 2 days", "Sent a notice.", 3);
        Post fixed = landlordPost(2L, "Mgmt fixed the heat fast this time", "Called Monday.", 2);
        Post slow = landlordPost(3L, "Landlord took 3 weeks to send a plumber", "Asked twice.", 1);
        Post none = landlordPost(4L, "The super is nice", "Nice guy.", 0);
        given(postRepository.findMentionsInBuilding(eq(10L), anyString())).willReturn(List.of(noReply, fixed, slow, none));
        given(postRepository.findAllById(List.of(2L))).willReturn(List.of(fixed));
        given(postTagRepository.findTagNamesByPostIds(List.of(2L))).willReturn(List.of());
        given(postImageRepository.findThumbnailKeysByPostIds(List.of(2L))).willReturn(List.of());
        given(postLikeRepository.countByPostIds(List.of(2L))).willReturn(List.of());
        given(replyRepository.countByPostIds(List.of(2L))).willReturn(List.of());
        LandlordPostSearchRequest request = new LandlordPostSearchRequest();
        request.setTopic(List.of(LandlordTopic.FIXED_WELL));

        LandlordPostListResponse response = communityService.getLandlordPosts(1L, request);

        assertThat(response.getAddress()).isEqualTo("312 E 21st St");
        assertThat(response.getTopicCounts()).containsEntry("ALL", 4L).containsEntry("NO_REPLY", 1L)
                .containsEntry("FIXED_WELL", 1L).containsEntry("SLOW_REPAIRS", 1L);
        assertThat(response.getContent()).singleElement().satisfies(p -> {
            assertThat(p.getPostId()).isEqualTo(2L);
            assertThat(p.getLandlordTopic()).isEqualTo(LandlordTopic.FIXED_WELL);
        });
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("이슈 관련 글: 카테고리 관련 태그로 조회 (HEAT -> noheat, radiator 등)")
    void issuePosts() {
        given(postRepository.findRelatedIdsInBuilding(eq(10L), any(), eq(5))).willReturn(List.of());

        assertThat(communityService.getIssuePosts(10L, IssueCategory.HEAT_HOT_WATER)).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keywords = ArgumentCaptor.forClass(List.class);
        verify(postRepository).findRelatedIdsInBuilding(eq(10L), keywords.capture(), eq(5));
        assertThat(keywords.getValue()).contains("noheat", "radiator");
    }

    // 분류·필터 테스트용 게시물 (쓰이지 않는 getter가 있어서 lenient)
    private Post landlordPost(Long id, String title, String content, int daysAgo) {
        Post post = mock(Post.class, withSettings().strictness(Strictness.LENIENT));
        given(post.getId()).willReturn(id);
        given(post.getTitle()).willReturn(title);
        given(post.getContent()).willReturn(content);
        given(post.getBoard()).willReturn(Board.FREE);
        given(post.getFloorBand()).willReturn(FloorBand.MIDDLE);
        given(post.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(daysAgo));
        return post;
    }

    @Test
    @DisplayName("상세: 다른 건물 게시물이면 POST_NOT_FOUND")
    void getPostOtherBuilding() {
        Post post = postIn(building(10L));
        UserPlace place = placeIn(building(20L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));

        assertThatThrownBy(() -> communityService.getPost(40L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(replyRepository, s3Uploader);
    }

    @Test
    @DisplayName("상세: 작성자는 층 구간 문구, 태그는 #, 이미지는 presigned URL, 댓글은 닉네임·이니셜")
    void getPost() {
        User me = author(1L);
        Post post = postIn(building(10L));
        given(post.getId()).willReturn(40L);
        given(post.getUser()).willReturn(me);
        given(post.getBoard()).willReturn(Board.FREE);
        given(post.getTitle()).willReturn("Floor 5 — our leak came back after repair");
        given(post.getContent()).willReturn("Same line as yours?");
        given(post.getFloorBand()).willReturn(FloorBand.MIDDLE);
        UserPlace place = placeIn(building(10L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));

        given(postTagRepository.findTagNamesByPost(post)).willReturn(List.of("ceilingleak", "floor5"));
        File image = mock(File.class);
        given(image.getId()).willReturn(130L);
        given(image.getStorageKey()).willReturn("post_image/1/abc");
        given(postImageRepository.findFilesByPostOrderBySortOrder(post)).willReturn(List.of(image));
        given(s3Uploader.getPresignedUrl("post_image/1/abc")).willReturn("https://signed");

        User neighbor = author(2L);
        given(neighbor.getNickname()).willReturn("GuiltyMo");
        Reply reply = replyOf(neighbor, FloorBand.LOWER);
        Reply deletedReply = replyOf(null, FloorBand.UPPER);
        given(replyRepository.findAllByPostWithUser(post)).willReturn(List.of(reply, deletedReply));

        given(postLikeRepository.countByIdPostId(40L)).willReturn(8L);
        given(postLikeRepository.existsByIdPostIdAndIdUserId(40L, 1L)).willReturn(false);

        PostDetailResponse response = communityService.getPost(40L, 1L);

        assertThat(response.getAuthor().getLabel()).isEqualTo("Middle Floor neighbor");
        assertThat(response.getAuthor().getIsMe()).isTrue();
        assertThat(response.getTags()).containsExactly("#ceilingleak", "#floor5");
        assertThat(response.getImages()).singleElement()
                .satisfies(i -> {
                    assertThat(i.getFileId()).isEqualTo(130L);
                    assertThat(i.getUrl()).isEqualTo("https://signed");
                });
        assertThat(response.getLikeCount()).isEqualTo(8L);
        assertThat(response.isLikedByMe()).isFalse();

        ReplyResponse first = response.getReplies().get(0);
        assertThat(first.getAuthor().getNickname()).isEqualTo("GuiltyMo");
        assertThat(first.getAuthor().getInitials()).isEqualTo("GM");
        assertThat(first.getAuthor().getFloorBand()).isEqualTo(FloorBand.LOWER);
        assertThat(first.getAuthor().getIsMe()).isFalse();

        ReplyResponse deleted = response.getReplies().get(1);
        assertThat(deleted.getAuthor().getNickname()).isEqualTo("Deleted user");
        assertThat(deleted.getAuthor().getInitials()).isNull();
        assertThat(deleted.getAuthor().getIsMe()).isFalse();
    }

    @Test
    @DisplayName("상세: 탈퇴한 작성자의 게시물은 Deleted user, isMe=false")
    void getPostOfDeletedUser() {
        Post post = postIn(building(10L));
        given(post.getUser()).willReturn(null);
        UserPlace place = placeIn(building(10L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));

        PostDetailResponse response = communityService.getPost(40L, 1L);

        assertThat(response.getAuthor().getLabel()).isEqualTo("Deleted user");
        assertThat(response.getAuthor().getIsMe()).isFalse();
    }

    @Test
    @DisplayName("댓글: 다른 건물 게시물이면 POST_NOT_FOUND")
    void createReplyOtherBuilding() {
        Post post = postIn(building(10L));
        UserPlace place = placeIn(building(20L));
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));

        assertThatThrownBy(() -> communityService.createReply(40L, 1L, replyRequest("hi")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verify(replyRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글: 집 등록이 없으면 POST_NOT_FOUND")
    void createReplyWithoutPlace() {
        Post post = mock(Post.class);
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.createReply(40L, 1L, replyRequest("hi")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
        verify(replyRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글: 앞뒤 공백을 지우고 작성자 층을 층 구간으로 저장 (8층 -> UPPER)")
    void createReply() {
        Post post = postIn(building(10L));
        User me = mock(User.class);
        UserPlace place = placeIn(building(10L));
        given(place.getUser()).willReturn(me);
        given(place.getFloor()).willReturn(8);
        given(postRepository.findById(40L)).willReturn(Optional.of(post));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Reply saved = mock(Reply.class);
        given(saved.getId()).willReturn(78L);
        given(replyRepository.save(any(Reply.class))).willReturn(saved);

        Long replyId = communityService.createReply(40L, 1L, replyRequest("  Same here.  ")).getReplyId();

        ArgumentCaptor<Reply> captor = ArgumentCaptor.forClass(Reply.class);
        verify(replyRepository).save(captor.capture());
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getUser()).isSameAs(me);
        assertThat(captor.getValue().getContent()).isEqualTo("Same here.");
        assertThat(captor.getValue().getFloorBand()).isEqualTo(FloorBand.UPPER);
        assertThat(replyId).isEqualTo(78L);
    }

    @Test
    @DisplayName("내 게시물: 지금 내 건물 글을 와프 카드 필드로, 탭 숫자(게시물 수 = 전체 개수, 댓글 수) 포함")
    void getMyPosts() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Post post = mock(Post.class, withSettings().strictness(Strictness.LENIENT));
        given(post.getId()).willReturn(40L);
        given(post.getTitle()).willReturn("Pipe burst in my unit");
        given(post.getBoard()).willReturn(Board.FREE);
        given(post.getContent()).willReturn("Water everywhere");
        given(postRepository.findByUserIdAndBuildingIdOrderByCreatedAtDescIdDesc(eq(1L), eq(10L), any()))
                .willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1));
        given(replyRepository.countByUserIdAndPostBuildingId(1L, 10L)).willReturn(5L);
        given(postRepository.findAllById(List.of(40L))).willReturn(List.of(post));
        given(postTagRepository.findTagNamesByPostIds(List.of(40L))).willReturn(List.<Object[]>of(new Object[]{40L, "ceilingleak"}));
        given(postImageRepository.findThumbnailKeysByPostIds(List.of(40L))).willReturn(List.of());
        given(postLikeRepository.countByPostIds(List.of(40L))).willReturn(List.<Object[]>of(new Object[]{40L, 9L}));
        given(replyRepository.countByPostIds(List.of(40L))).willReturn(List.<Object[]>of(new Object[]{40L, 2L}));

        MyPostListResponse response = communityService.getMyPosts(1L, 0, 20);

        assertThat(response.getCounts().getPosts()).isEqualTo(1L);
        assertThat(response.getCounts().getReplies()).isEqualTo(5L);
        assertThat(response.getContent()).singleElement().satisfies(card -> {
            assertThat(card.getPostId()).isEqualTo(40L);
            assertThat(card.getTitle()).isEqualTo("Pipe burst in my unit");
            assertThat(card.getBoard()).isEqualTo(Board.FREE);
            assertThat(card.getTags()).containsExactly("#ceilingleak");
            assertThat(card.getLikeCount()).isEqualTo(9L);
            assertThat(card.getReplyCount()).isEqualTo(2L);
        });
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("내 게시물: 집 등록이 없으면 PLACE_NOT_REGISTERED")
    void getMyPostsWithoutPlace() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getMyPosts(1L, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("내 댓글: 지금 내 건물 글에 단 댓글을 게시물 제목과 함께")
    void getMyReplies() {
        UserPlace place = placeIn(building(10L));
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.of(place));
        Post post = mock(Post.class);
        given(post.getId()).willReturn(31L);
        given(post.getTitle()).willReturn("Floor 5 — our leak came back after repair");
        Reply reply = mock(Reply.class);
        given(reply.getId()).willReturn(77L);
        given(reply.getPost()).willReturn(post);
        given(reply.getContent()).willReturn("Same here. Ours started after the repair too.");
        given(replyRepository.findByUserIdAndPostBuildingIdOrderByCreatedAtDescIdDesc(eq(1L), eq(10L), any()))
                .willReturn(new PageImpl<>(List.of(reply), PageRequest.of(0, 20), 1));

        MyReplyListResponse response = communityService.getMyReplies(1L, 0, 20);

        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getContent()).singleElement().satisfies(card -> {
            assertThat(card.getReplyId()).isEqualTo(77L);
            assertThat(card.getPostId()).isEqualTo(31L);
            assertThat(card.getPostTitle()).isEqualTo("Floor 5 — our leak came back after repair");
            assertThat(card.getContent()).isEqualTo("Same here. Ours started after the repair too.");
        });
    }

    @Test
    @DisplayName("내 댓글: 집 등록이 없으면 PLACE_NOT_REGISTERED")
    void getMyRepliesWithoutPlace() {
        given(userPlaceRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getMyReplies(1L, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(CommunityErrorCode.PLACE_NOT_REGISTERED);
        verifyNoInteractions(replyRepository);
    }

    private ReplyCreateRequest replyRequest(String content) {
        ReplyCreateRequest request = new ReplyCreateRequest();
        request.setContent(content);
        return request;
    }

    private Reply replyOf(User user, FloorBand floorBand) {
        Reply reply = mock(Reply.class);
        given(reply.getUser()).willReturn(user);
        given(reply.getFloorBand()).willReturn(floorBand);
        return reply;
    }

    private PostLikeRequest likeRequest(boolean liked) {
        PostLikeRequest request = new PostLikeRequest();
        request.setLiked(liked);
        return request;
    }

    private Building building(Long id) {
        Building building = mock(Building.class);
        given(building.getId()).willReturn(id);
        return building;
    }

    private Post postIn(Building building) {
        Post post = mock(Post.class);
        given(post.getBuilding()).willReturn(building);
        return post;
    }

    private UserPlace placeIn(Building building) {
        UserPlace place = mock(UserPlace.class);
        given(place.getBuilding()).willReturn(building);
        return place;
    }

    private User author(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private Post postOf(User user) {
        Post post = mock(Post.class);
        given(post.getUser()).willReturn(user);
        return post;
    }
}
