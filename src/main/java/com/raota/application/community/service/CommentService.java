package com.raota.application.community.service;

import com.raota.domain.community.model.Comment;
import com.raota.presentation.api.community.request.CommunityCommentCreateRequest;
import com.raota.presentation.api.community.request.CommunityCommentUpdateRequest;
import com.raota.domain.community.repository.command.CommentRepository;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.CommentEntity;
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

        // 답글인 경우 부모 댓글 존재 및 Depth(1) 체크
        if (request.getParentCommentId() != null) {
            CommentEntity parent = commentRepository.findEntityById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
            
            if (parent.getParent() != null) {
                throw new IllegalArgumentException("답글에는 답글을 달 수 없습니다. (최대 Depth 1)");
            }
        }

        Comment comment = Comment.create(postId, authorId, request.getParentCommentId(), request.getContent());
        Comment savedComment = commentRepository.save(comment);
        
        if (commentRepository instanceof com.raota.domain.community.repository.command.JpaCommentRepository jpaRepo) {
            jpaRepo.flush();
        }

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increaseCommentCount();

        return savedComment.getId();
    }

    public void updateComment(Long commentId, CommunityCommentUpdateRequest request, Long authorId) {
        CommentEntity commentEntity = commentRepository.findEntityById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!commentEntity.getMember().getId().equals(authorId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        commentEntity.update(request.getContent());
    }

    public void deleteComment(Long commentId, Long authorId) {
        CommentEntity commentEntity = commentRepository.findEntityById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!commentEntity.getMember().getId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        commentEntity.delete();

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreaseCommentCount();
    }
}
