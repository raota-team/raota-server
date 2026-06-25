package com.raota.application.community.port;

import com.raota.application.community.result.CommentItemResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryPort {

    Optional<CommentItemResult> getComment(Long commentId);

    Page<CommentItemResult> getParentComments(Long postId, Pageable pageable);

    List<CommentItemResult> getReplies(Long parentId);
}
