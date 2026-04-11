package com.raota.domain.community.service;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.repository.command.CommentRepository;
import com.raota.domain.community.repository.command.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public Long createComment(Long postId, CommunityCommentCreateRequest request, Long authorId) {
        // 게시글 존재 여부 확인
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Comment comment = Comment.create(postId, authorId, request.getContent());
        return commentRepository.save(comment).getId();
    }

    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        commentRepository.delete(commentId);
    }
}
