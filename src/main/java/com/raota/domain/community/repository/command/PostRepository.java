package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Post;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    void delete(Long id);
}
