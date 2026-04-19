package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.repository.command.entity.PostEntity;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
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
}
