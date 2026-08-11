package com.raota.community.domain.repository;

import com.raota.community.domain.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    void validateReplyTarget(Long parentId);
    void update(Long id, Long authorId, String content);
    void softDelete(Long id, Long authorId);
    void delete(Long id);
    void deleteAll();
    List<Comment> findAllByPostId(Long postId);
}
