package com.raota.community.infrastructure.persistence.command;

import com.raota.community.domain.model.Post;
import com.raota.community.domain.model.PostCategory;
import com.raota.community.domain.repository.PostRepository;
import com.raota.community.infrastructure.persistence.entity.PostEntity;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface JpaPostEntityRepository extends JpaRepository<PostEntity, Long> {}

@Repository
@RequiredArgsConstructor
public class JpaPostRepository implements PostRepository {
    private final JpaPostEntityRepository jpaRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;

    @Override
    public Post save(Post post) {
        MemberProfile author = memberRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다."));
        
        RamenShop ramenShop = post.getRamenShopId() != null 
                ? ramenShopRepository.findById(post.getRamenShopId()).orElse(null) 
                : null;

        PostEntity entity = PostEntity.fromDomain(post, author, ramenShop);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return jpaRepository.findById(id).map(PostEntity::toDomain);
    }

    @Override
    public PostUpdateResult update(
            Long id,
            Long authorId,
            PostCategory category,
            String title,
            String content,
            String thumbnailUrl,
            Long ramenShopId
    ) {
        PostEntity postEntity = findPostEntity(id);

        if (!postEntity.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        PostCategory beforeCategory = postEntity.getCategory();
        RamenShop ramenShop = null;

        if (ramenShopId != null) {
            ramenShop = ramenShopRepository.findById(ramenShopId)
                    .orElseThrow(() -> new IllegalArgumentException("없는 라멘집 입니다."));
        }

        postEntity.update(category, title, content, thumbnailUrl, ramenShop);

        return new PostUpdateResult(postEntity.getId(), beforeCategory, postEntity.getCategory());
    }

    @Override
    public PostCategory delete(Long id, Long authorId) {
        PostEntity postEntity = findPostEntity(id);

        if (!postEntity.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        PostCategory category = postEntity.getCategory();
        postEntity.delete();
        return category;
    }

    @Override
    public void increaseViewCount(Long id) {
        PostEntity postEntity = findPostEntity(id);

        if (postEntity.isDeleted()) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        postEntity.increaseViewCount();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    public void flush() {
        jpaRepository.flush();
    }

    private PostEntity findPostEntity(Long id) {
        return jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }
}
