package com.raota.application.community.service;

import com.raota.application.community.command.CreatePostCommand;
import com.raota.application.community.command.UpdatePostCommand;
import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.PostEntity;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.event.PostIndexingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Long createPost(CreatePostCommand command) {
        Post post = Post.create(
                PostCategory.valueOf(command.category()),
                command.title(),
                command.content(),
                command.contentFormat(),
                command.thumbnailUrl(),
                command.authorId(),
                command.ramenShopId()
        );

        Post savedPost = postRepository.save(post);

        Long postId = savedPost.getId();

        MemberProfile author = memberRepository.findById(command.authorId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increasePostCount();

        if (savedPost.getCategory() == PostCategory.REVIEW) {
            eventPublisher.publishEvent(PostIndexingEvent.upsert(savedPost.getId()));
        }

        return postId;
    }

    public void updatePost(UpdatePostCommand command) {
        PostEntity postEntity = postRepository.findEntityById(command.postId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!postEntity.getAuthor().getId().equals(command.authorId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        PostCategory beforeCategory = postEntity.getCategory();
        RamenShop ramenShop = null;

        if (command.ramenShopId() != null) {
            ramenShop = ramenShopRepository.findById(command.ramenShopId())
                    .orElseThrow(() -> new IllegalArgumentException("없는 라멘집 입니다."));
        }

        postEntity.update(
                PostCategory.valueOf(command.category()),
                command.title(),
                command.content(),
                command.thumbnailUrl(),
                ramenShop
        );

        PostCategory afterCategory = postEntity.getCategory();
        if (beforeCategory == PostCategory.REVIEW || afterCategory == PostCategory.REVIEW) {
            eventPublisher.publishEvent(PostIndexingEvent.upsert(postEntity.getId()));
        }
    }

    public void deletePost(Long postId, Long authorId) {
        PostEntity postEntity = postRepository.findEntityById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        if (!postEntity.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        PostCategory category = postEntity.getCategory();

        postEntity.delete();

        if (category == PostCategory.REVIEW) {
            eventPublisher.publishEvent(PostIndexingEvent.delete(postId));
        }

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreasePostCount();
    }

    public void increaseViewCount(Long postId) {
        PostEntity postEntity = postRepository.findEntityById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (postEntity.isDeleted()) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        postEntity.increaseViewCount();
    }
}
