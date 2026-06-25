package com.raota.domain.community.repository;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    PostUpdateResult update(Long id, Long authorId, PostCategory category, String title, String content, String thumbnailUrl, Long ramenShopId);
    PostCategory delete(Long id, Long authorId);
    void increaseViewCount(Long id);
    void delete(Long id);
    void deleteAll();

    record PostUpdateResult(Long postId, PostCategory beforeCategory, PostCategory afterCategory) {
    }
}
