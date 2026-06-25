package com.raota.application.community.service;

import com.raota.application.community.port.PostQueryPort;
import com.raota.application.community.query.PostSearchQuery;
import com.raota.application.community.query.RamenShopOptionSearchQuery;
import com.raota.application.community.result.HomePostResult;
import com.raota.application.community.result.PopularPostResult;
import com.raota.application.community.result.PostCardResult;
import com.raota.application.community.result.PostDetailResult;
import com.raota.application.community.result.RamenShopOptionResult;
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

    public PostDetailResult getPostDetail(Long postId, Long memberId) {
        return postQueryPort.getPostDetail(postId, memberId);
    }

    public Page<RamenShopOptionResult> getRamenShopOptions(RamenShopOptionSearchQuery query, Pageable pageable) {
        return postQueryPort.getRamenShopOptions(query, pageable);
    }

    public List<HomePostResult> findHomePosts(String category, int limit) {
        return postQueryPort.findHomePosts(category, limit);
    }

    public List<PopularPostResult> findRecentPopularPosts(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 10));
        return postQueryPort.findRecentPopularPosts(normalizedLimit);
    }
}
