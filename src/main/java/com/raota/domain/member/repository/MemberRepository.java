package com.raota.domain.member.repository;

import com.raota.presentation.api.member.dto.BookmarkSummaryResponse;
import com.raota.presentation.api.member.dto.MyProfileResponse;
import com.raota.presentation.api.member.dto.PhotoSummaryResponse;
import com.raota.presentation.api.member.dto.VisitSummaryResponse;
import com.raota.domain.member.model.MemberProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberProfile, Long> {
    @Query("""
    select new com.raota.presentation.api.member.dto.MyProfileResponse(
        m.id,
        m.nickname,
        m.imageUrl,
        m.backgroundImageUrl,
        m.bio,
        new com.raota.presentation.api.member.dto.UserStatsDto(
            (select count(distinct p.ramenShop.id) from RamenProofPicture p where p.memberProfile.id = m.id and p.isDeleted = false),
            (select count(p) from RamenProofPicture p where p.memberProfile.id = m.id and p.isDeleted = false),
            (select count(b) from Bookmark b where b.memberProfile.id = m.id and b.isDeleted = false),
            (select count(p) from PostEntity p where p.author.id = m.id and p.isDeleted = false),
            (select count(c) from CommentEntity c where c.member.id = m.id and c.isDeleted = false and c.post.isDeleted = false)
        )
    )
    from MemberProfile m
    where m.id = :id
""")
    MyProfileResponse findMemberDetailInfo(@Param("id") Long id);

    @Query(value = """
            select new com.raota.presentation.api.community.dto.CommunityPostCardResponse(
                p.id,
                cast(p.category as string),
                r.id,
                r.name,
                p.title,
                p.content,
                p.thumbnailUrl,
                m.nickname,
                m.id,
                m.imageUrl,
                p.createdAt,
                (select count(l) from PostLikeEntity l where l.postId = p.id),
                (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false)
            )
            from PostEntity p
            join p.author m
            left join p.ramenShop r
            where m.id = :memberId and p.isDeleted = false
            order by p.createdAt desc
            """,
            countQuery = """
                    select count(p)
                    from PostEntity p
                    where p.author.id = :memberId and p.isDeleted = false
                    """)
    Page<com.raota.presentation.api.community.dto.CommunityPostCardResponse> findMyPosts(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.community.dto.CommunityCommentItemResponse(
                c.id,
                c.parent.id,
                c.post.id,
                m.nickname,
                m.id,
                m.imageUrl,
                null,
                c.createdAt,
                c.content,
                c.isDeleted
            )
            from CommentEntity c
            join c.member m
            where m.id = :memberId
                and c.isDeleted = false
                and c.post.isDeleted = false
            order by c.createdAt desc
            """,
            countQuery = """
                    select count(c)
                    from CommentEntity c
                    where c.member.id = :memberId
                        and c.isDeleted = false
                        and c.post.isDeleted = false
                    """)
    Page<com.raota.presentation.api.community.dto.CommunityCommentItemResponse> findMyComments(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.member.dto.VisitSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                CONCAT(CONCAT(r.address.city, ' '), r.address.district),
                count(p.id),
                max(p.uploadedAt))
            from MemberProfile m
            join RamenProofPicture p on p.memberProfile = m
            join p.ramenShop r
            where m.id = :memberId and p.isDeleted = false
            group by r.id, r.name, r.imageUrl, r.address.city, r.address.district
            having count(p.id) > 0
            order by count(p.id) desc
            """,
            countQuery = """
                    select count(distinct r.id)
                    from MemberProfile m
                    join RamenProofPicture p on p.memberProfile = m
                    join p.ramenShop r
                    where m.id = :memberId and p.isDeleted = false
                    """)
    Page<VisitSummaryResponse> findMyVisitRestaurant(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.member.dto.BookmarkSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                concat(r.address.city, concat(' ', r.address.district)),
                b.markingAt
            )
            from MemberProfile m
            join Bookmark b on b.memberProfile = m
            join b.ramenShop r
            where m.id = :memberId and b.isDeleted = false
            order by b.markingAt desc
            """,
            countQuery = """
                    select count(b)
                    from MemberProfile m
                    join Bookmark b on b.memberProfile = m
                    where m.id = :memberId and b.isDeleted = false
                    """)
    Page<BookmarkSummaryResponse> findMyBookmarks(@Param("memberId") Long memberId, Pageable pageable);

    @Query(value = """
    select new com.raota.presentation.api.member.dto.PhotoSummaryResponse(
        p.id,
        p.imageUrl,
        p.menuName,
        p.description,
        p.ramenShop.id,
        p.ramenShop.name,
        p.uploadedAt
    )
    from RamenProofPicture p
    where p.memberProfile.id = :memberId and p.isDeleted = false
""",
            countQuery = """
    select count(p)
    from RamenProofPicture p
    where p.memberProfile.id = :memberId and p.isDeleted = false
""")
    Page<PhotoSummaryResponse> findMyPhotos(@Param("memberId") Long memberId, Pageable pageable);
}
