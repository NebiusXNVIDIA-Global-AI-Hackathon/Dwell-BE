package com.dwell.domain.community.service;

import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.community.dto.request.PostLikeRequest;
import com.dwell.domain.community.dto.request.ReplyCreateRequest;
import com.dwell.domain.community.dto.response.PostDetailResponse;
import com.dwell.domain.community.dto.response.PostLikeResponse;
import com.dwell.domain.community.dto.response.ReplyResponse;
import com.dwell.domain.community.entity.Reply;
import com.dwell.domain.community.enums.Board;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    private CommunityServiceImpl communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityServiceImpl(postRepository, tagRepository, postTagRepository, postImageRepository,
                postLikeRepository, replyRepository, userPlaceRepository, fileRepository, fileService, s3Uploader);
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
