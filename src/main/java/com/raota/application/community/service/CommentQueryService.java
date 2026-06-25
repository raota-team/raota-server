package com.raota.application.community.service;

import com.raota.application.community.port.CommentQueryPort;
import com.raota.application.community.result.CommentItemResult;
import com.raota.application.community.result.CommentThreadResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentQueryPort commentQueryPort;

    public CommentItemResult getComment(Long commentId) {
        return commentQueryPort.getComment(commentId)
                .orElseThrow(() -> new IllegalStateException("댓글을 찾을 수 없습니다."));
    }

    public Page<CommentThreadResult> getCommentThreads(Long postId, Pageable pageable) {
        Page<CommentItemResult> parents = commentQueryPort.getParentComments(postId, pageable);
        List<CommentThreadResult> threads = parents.getContent().stream()
                .map(parent -> CommentThreadResult.of(parent, commentQueryPort.getReplies(parent.commentId())))
                .toList();

        return new PageImpl<>(threads, pageable, parents.getTotalElements());
    }
}
