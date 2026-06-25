package com.raota.application.community.port;

import com.raota.application.community.query.PostSearchQuery;
import com.raota.application.community.query.RamenShopOptionSearchQuery;
import com.raota.application.community.result.HomePostResult;
import com.raota.application.community.result.PopularPostResult;
import com.raota.application.community.result.PostCardResult;
import com.raota.application.community.result.PostDetailResult;
import com.raota.application.community.result.RamenShopOptionResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryPort {

    Page<PostCardResult> searchPostCards(PostSearchQuery query, Pageable pageable);

    PostDetailResult getPostDetail(Long postId, Long memberId);

    Page<RamenShopOptionResult> getRamenShopOptions(RamenShopOptionSearchQuery query, Pageable pageable);

    List<HomePostResult> findHomePosts(String categoryName, int limit);

    List<PopularPostResult> findRecentPopularPosts(int limit);
}
