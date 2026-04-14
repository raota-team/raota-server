package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    void delete(Long id);
    List<Comment> findAllByPostId(Long postId);
}
