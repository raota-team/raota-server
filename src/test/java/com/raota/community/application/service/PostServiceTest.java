package com.raota.community.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.raota.community.application.command.CreatePostCommand;
import com.raota.community.application.command.UpdatePostCommand;
import com.raota.community.domain.model.Post;
import com.raota.community.domain.model.PostCategory;
import com.raota.community.domain.repository.PostRepository;
import com.raota.community.application.service.PostService;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.community.domain.event.PostIndexingAction;
import com.raota.community.domain.event.PostIndexingEvent;
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
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private PostService postService;

    @Test
    @DisplayName("리뷰 게시글 작성 시 인덱싱 이벤트가 발행된다.")
    void create_review_post_publishes_event() {
        // given
        Long authorId = 1L;
        Long savedPostId = 100L;

        CreatePostCommand command = new CreatePostCommand(
                "REVIEW", null, "제목", null, "PLAIN", "내용", authorId
        );

        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        Post savedPost = mock(Post.class);
        when(savedPost.getId()).thenReturn(savedPostId);
        when(savedPost.getCategory()).thenReturn(PostCategory.REVIEW);
        
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // when
        postService.createPost(command);

        // then
        ArgumentCaptor<PostIndexingEvent> eventCaptor = ArgumentCaptor.forClass(PostIndexingEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostIndexingEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.postId()).isEqualTo(savedPostId);
        assertThat(publishedEvent.action()).isEqualTo(PostIndexingAction.UPSERT);
    }

    @Test
    @DisplayName("리뷰가 아닌 일반 게시글 작성 시 인덱싱 이벤트가 발행되지 않는다.")
    void create_free_post_does_not_publish_event() {
        // given
        Long authorId = 1L;

        CreatePostCommand command = new CreatePostCommand(
                "FREE", null, "제목", null, "PLAIN", "내용", authorId
        );

        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        Post savedPost = mock(Post.class);
        when(savedPost.getId()).thenReturn(100L);
        when(savedPost.getCategory()).thenReturn(PostCategory.FREE);
        
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // when
        postService.createPost(command);

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

        UpdatePostCommand command = new UpdatePostCommand(
                postId, "FREE", null, "제목", null, "PLAIN", "내용", otherId
        );
        when(postRepository.update(
                postId,
                otherId,
                PostCategory.FREE,
                "제목",
                "내용",
                null,
                null
        )).thenThrow(new IllegalStateException("수정 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("수정 권한이 없습니다.");
    }

    @Test
    @DisplayName("리뷰 게시글 수정 시 인덱싱 갱신 이벤트가 발행된다.")
    void update_review_post_publishes_upsert_event() {
        // given
        Long postId = 1L;
        Long authorId = 1L;

        UpdatePostCommand command = new UpdatePostCommand(
                postId, "REVIEW", null, "수정 제목", null, "PLAIN", "수정 내용", authorId
        );
        when(postRepository.update(
                postId,
                authorId,
                PostCategory.REVIEW,
                "수정 제목",
                "수정 내용",
                null,
                null
        )).thenReturn(new PostRepository.PostUpdateResult(postId, PostCategory.REVIEW, PostCategory.REVIEW));

        // when
        postService.updatePost(command);

        // then
        ArgumentCaptor<PostIndexingEvent> eventCaptor = ArgumentCaptor.forClass(PostIndexingEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostIndexingEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.postId()).isEqualTo(postId);
        assertThat(publishedEvent.action()).isEqualTo(PostIndexingAction.UPSERT);
    }

    @Test
    @DisplayName("본인의 게시글은 소프트 딜리트 처리된다.")
    void delete_post_success() {
        // given
        Long postId = 1L;
        Long authorId = 1L;

        when(postRepository.delete(postId, authorId)).thenReturn(PostCategory.FREE);
        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // when
        postService.deletePost(postId, authorId);

        // then
        verify(postRepository).delete(postId, authorId);
        verify(author).decreasePostCount();
    }

    @Test
    @DisplayName("리뷰 게시글 삭제 시 인덱싱 삭제 이벤트가 발행된다.")
    void delete_review_post_publishes_delete_event() {
        // given
        Long postId = 1L;
        Long authorId = 1L;

        when(postRepository.delete(postId, authorId)).thenReturn(PostCategory.REVIEW);
        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // when
        postService.deletePost(postId, authorId);

        // then
        ArgumentCaptor<PostIndexingEvent> eventCaptor = ArgumentCaptor.forClass(PostIndexingEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PostIndexingEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.postId()).isEqualTo(postId);
        assertThat(publishedEvent.action()).isEqualTo(PostIndexingAction.DELETE);
    }
}
