package com.raota.domain.community.repository.command.entity;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.ramenShop.model.RamenShop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String contentFormat;

    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberProfile author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_shop_id")
    private RamenShop ramenShop;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PostEntity fromDomain(Post post, MemberProfile author, RamenShop ramenShop) {
        return PostEntity.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .contentFormat(post.getContentFormat())
                .thumbnailUrl(post.getThumbnailUrl())
                .author(author)
                .ramenShop(ramenShop)
                .createdAt(post.getCreatedAt())
                .build();
    }

    public Post toDomain() {
        return Post.of(
                id,
                category,
                title,
                content,
                contentFormat,
                thumbnailUrl,
                author.getId(),
                ramenShop != null ? ramenShop.getId() : null,
                createdAt
        );
    }
}
