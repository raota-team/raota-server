package com.raota.unit.application.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.raota.application.community.command.CreateCommentCommand;
import com.raota.domain.community.repository.CommentRepository;
import com.raota.domain.community.repository.PostRepository;
import com.raota.application.community.service.CommentService;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private MemberRepository memberRepository;
    @InjectMocks private CommentService commentService;

    @Test
    @DisplayName("답글에 다시 답글을 달려고 하면 예외가 발생한다.")
    void reply_to_reply_fail() {
        // given
        Long postId = 1L;
        Long parentId = 10L;
        Long memberId = 1L;

        when(postRepository.findById(postId)).thenReturn(Optional.of(mock(com.raota.domain.community.model.Post.class)));
        doThrow(new IllegalArgumentException("답글에는 답글을 달 수 없습니다. (최대 Depth 1)"))
                .when(commentRepository)
                .validateReplyTarget(parentId);

        CreateCommentCommand command = new CreateCommentCommand(postId, memberId, parentId, "답글의 답글");

        // when & then
        assertThatThrownBy(() -> commentService.createComment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("답글에는 답글을 달 수 없습니다.");
    }

    @Test
    @DisplayName("본인의 댓글은 소프트 딜리트 처리된다.")
    void delete_comment_success() {
        // given
        Long commentId = 1L;
        Long authorId = 1L;

        MemberProfile author = mock(MemberProfile.class);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // when
        commentService.deleteComment(commentId, authorId);

        // then
        verify(commentRepository).softDelete(commentId, authorId);
        verify(author).decreaseCommentCount();
    }
}
