package com.raota.community.application.port;

import com.raota.community.application.query.PostSearchQuery;
import com.raota.community.application.result.HomePostResult;
import com.raota.community.application.result.PopularPostResult;
import com.raota.community.application.result.PostCardResult;
import com.raota.community.application.result.PostDetailResult;
import com.raota.community.application.result.RamenShopOptionResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryPort {

    Page<PostCardResult> searchPostCards(PostSearchQuery query, Pageable pageable);

    Page<PostCardResult> findPostCardsByAuthor(Long authorId, Pageable pageable);

    PostDetailResult getPostDetail(Long postId, Long memberId);

    Page<RamenShopOptionResult> getRamenShopOptions(String keyword, Pageable pageable);

    List<HomePostResult> findHomePosts(String categoryName, int limit);

    List<PopularPostResult> findRecentPopularPosts(int limit);
}
