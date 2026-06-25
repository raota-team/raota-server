package com.raota.application.community.service;

import com.raota.application.community.command.CreatePostCommand;
import com.raota.application.community.command.UpdatePostCommand;
import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.repository.PostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
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
        PostRepository.PostUpdateResult result = postRepository.update(
                command.postId(),
                command.authorId(),
                PostCategory.valueOf(command.category()),
                command.title(),
                command.content(),
                command.thumbnailUrl(),
                command.ramenShopId()
        );

        if (result.beforeCategory() == PostCategory.REVIEW || result.afterCategory() == PostCategory.REVIEW) {
            eventPublisher.publishEvent(PostIndexingEvent.upsert(result.postId()));
        }
    }

    public void deletePost(Long postId, Long authorId) {
        PostCategory category = postRepository.delete(postId, authorId);

        if (category == PostCategory.REVIEW) {
            eventPublisher.publishEvent(PostIndexingEvent.delete(postId));
        }

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreasePostCount();
    }

    public void increaseViewCount(Long postId) {
        postRepository.increaseViewCount(postId);
    }
}
