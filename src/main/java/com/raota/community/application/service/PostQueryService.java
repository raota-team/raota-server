package com.raota.community.application.service;

import com.raota.community.application.port.PostQueryPort;
import com.raota.community.application.query.PostSearchQuery;
import com.raota.community.application.result.HomePostResult;
import com.raota.community.application.result.PopularPostResult;
import com.raota.community.application.result.PostCardResult;
import com.raota.community.application.result.PostDetailResult;
import com.raota.community.application.result.RamenShopOptionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostQueryPort postQueryPort;

    public Page<PostCardResult> searchPostCards(PostSearchQuery query, Pageable pageable) {
        return postQueryPort.searchPostCards(query, pageable);
    }

    public Page<PostCardResult> findPostCardsByAuthor(Long authorId, Pageable pageable) {
        return postQueryPort.findPostCardsByAuthor(authorId, pageable);
    }

    public PostDetailResult getPostDetail(Long postId, Long memberId) {
        return postQueryPort.getPostDetail(postId, memberId);
    }

    public Page<RamenShopOptionResult> getRamenShopOptions(String keyword, Pageable pageable) {
        return postQueryPort.getRamenShopOptions(keyword, pageable);
    }

    public List<HomePostResult> findHomePosts(String category, int limit) {
        return postQueryPort.findHomePosts(category, limit);
    }

    public List<PopularPostResult> findRecentPopularPosts(int limit) {
        int normalizedLimit = Math.clamp(limit, 1, 10);
        return postQueryPort.findRecentPopularPosts(normalizedLimit);
    }
}
