package com.dwell.domain.community.service;

import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.community.entity.Post;
import com.dwell.domain.community.exception.CommunityErrorCode;
import com.dwell.domain.community.repository.PostImageRepository;
import com.dwell.domain.community.repository.PostRepository;
import com.dwell.domain.community.repository.PostTagRepository;
import com.dwell.domain.community.repository.TagRepository;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.FileService;
import com.dwell.domain.user.entity.User;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    private UserPlaceRepository userPlaceRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileService fileService;

    private CommunityServiceImpl communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityServiceImpl(postRepository, tagRepository, postTagRepository, postImageRepository,
                userPlaceRepository, fileRepository, fileService);
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
