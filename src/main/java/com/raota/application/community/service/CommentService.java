package com.raota.application.community.service;

import com.raota.application.community.command.CreateCommentCommand;
import com.raota.application.community.command.UpdateCommentCommand;
import com.raota.domain.community.model.Comment;
import com.raota.domain.community.repository.CommentRepository;
import com.raota.domain.community.repository.PostRepository;
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

    public Long createComment(CreateCommentCommand command) {
        postRepository.findById(command.postId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        commentRepository.validateReplyTarget(command.parentCommentId());

        Comment comment = Comment.create(command.postId(), command.authorId(), command.parentCommentId(), command.content());
        Comment savedComment = commentRepository.save(comment);

        MemberProfile author = memberRepository.findById(command.authorId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increaseCommentCount();

        return savedComment.getId();
    }

    public void updateComment(UpdateCommentCommand command) {
        commentRepository.update(command.commentId(), command.authorId(), command.content());
    }

    public void deleteComment(Long commentId, Long authorId) {
        commentRepository.softDelete(commentId, authorId);

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreaseCommentCount();
    }
}
