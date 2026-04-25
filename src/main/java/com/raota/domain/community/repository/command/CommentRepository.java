package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.repository.command.entity.CommentEntity;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    Optional<CommentEntity> findEntityById(Long id);
    void delete(Long id);
    void deleteAll();
    List<Comment> findAllByPostId(Long postId);
}
