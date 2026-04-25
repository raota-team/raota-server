package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Post;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    Optional<com.raota.domain.community.repository.command.entity.PostEntity> findEntityById(Long id);
    void delete(Long id);
    void deleteAll();
}
