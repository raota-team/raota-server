package com.raota.domain.community.service;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.presentation.request.CommunityCommentUpdateRequest;
import com.raota.domain.community.repository.command.CommentRepository;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public Long createComment(Long postId, CommunityCommentCreateRequest request, Long authorId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Comment comment = Comment.create(postId, authorId, request.getContent());
        Long commentId = commentRepository.save(comment).getId();

        // 마이페이지 통계 업데이트
        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increaseCommentCount();

        return commentId;
    }

    public void updateComment(Long commentId, CommunityCommentUpdateRequest request, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        Comment updatedComment = comment.update(request.getContent());
        commentRepository.save(updatedComment);
    }

    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        commentRepository.delete(commentId);

        // 마이페이지 통계 업데이트
        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreaseCommentCount();
    }
}
