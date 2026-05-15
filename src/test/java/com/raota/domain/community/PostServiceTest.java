package com.raota.domain.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.PostEntity;
import com.raota.domain.community.service.PostService;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private RamenShopRepository ramenShopRepository;
    @InjectMocks private PostService postService;

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
