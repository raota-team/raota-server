package com.raota.domain.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.PostEntity;
import com.raota.domain.community.service.PostService;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.event.PostIndexingEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private RamenShopRepository ramenShopRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private PostService postService;

    @Test
    @DisplayName("리뷰 게시글 작성 시 인덱싱 이벤트가 발행된다.")
    void create_review_post_publishes_event() {
        // given
        Long authorId = 1L;
        Long savedPostId = 100L;

        CommunityPostCreateRequest request = new CommunityPostCreateRequest(
                "REVIEW", null, "제목", null, "PLAIN", "내용"
        );

        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        Post savedPost = mock(Post.class);
        when(savedPost.getId()).thenReturn(savedPostId);
        when(savedPost.getCategory()).thenReturn(PostCategory.REVIEW);
        
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // when
        postService.createPost(request, authorId);

        // then
        ArgumentCaptor<PostIndexingEvent> eventCaptor = ArgumentCaptor.forClass(PostIndexingEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostIndexingEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.postId()).isEqualTo(savedPostId);
    }

    @Test
    @DisplayName("리뷰가 아닌 일반 게시글 작성 시 인덱싱 이벤트가 발행되지 않는다.")
    void create_free_post_does_not_publish_event() {
        // given
        Long authorId = 1L;

        CommunityPostCreateRequest request = new CommunityPostCreateRequest(
                "FREE", null, "제목", null, "PLAIN", "내용"
        );

        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        Post savedPost = mock(Post.class);
        when(savedPost.getId()).thenReturn(100L);
        when(savedPost.getCategory()).thenReturn(PostCategory.FREE);
        
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // when
        postService.createPost(request, authorId);

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("다른 사람의 게시글을 수정하려 하면 예외가 발생한다.")
    void update_post_permission_fail() {
        // given
        Long postId = 1L;
        Long authorId = 1L;
        Long otherId = 2L;

        MemberProfile author = mock(MemberProfile.class);
        when(author.getId()).thenReturn(authorId);

        PostEntity post = mock(PostEntity.class);
        when(post.getAuthor()).thenReturn(author);

        when(postRepository.findEntityById(postId)).thenReturn(Optional.of(post));

        CommunityPostCreateRequest request = new CommunityPostCreateRequest(
                "FREE", null, "제목", null, "PLAIN", "내용"
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(postId, request, otherId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("수정 권한이 없습니다.");
    }

    @Test
    @DisplayName("본인의 게시글은 소프트 딜리트 처리된다.")
    void delete_post_success() {
        // given
        Long postId = 1L;
        Long authorId = 1L;

        MemberProfile author = mock(MemberProfile.class);
        when(author.getId()).thenReturn(authorId);

        PostEntity post = mock(PostEntity.class);
        when(post.getAuthor()).thenReturn(author);

        when(postRepository.findEntityById(postId)).thenReturn(Optional.of(post));
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // when
        postService.deletePost(postId, authorId);

        // then
        verify(post).delete();
        verify(author).decreasePostCount();
    }
}