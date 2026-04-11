package com.raota.domain.member.repository;

import com.raota.domain.member.controller.response.BookmarkSummaryResponse;
import com.raota.domain.member.controller.response.MyProfileResponse;
import com.raota.domain.member.controller.response.PhotoSummaryResponse;
import com.raota.domain.member.controller.response.VisitSummaryResponse;
import com.raota.domain.member.model.MemberProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberProfile, Long> {
    @Query("""
    select new com.raota.domain.member.controller.response.MyProfileResponse(
        m.id,
        m.nickname,
        m.imageUrl,
        m.backgroundImageUrl,
        m.nickname,
        new com.raota.domain.member.dto.UserStatsDto(
            m.memberActivityStats.visitedRestaurantCount,
            m.memberActivityStats.photoCount,
            m.memberActivityStats.bookmarkCount,
            m.memberActivityStats.postCount,
            m.memberActivityStats.commentCount
        )
    )
    from MemberProfile m
    where m.id = :id
""")
    MyProfileResponse findMemberDetailInfo(@Param("id") Long id);

    @Query(value = """
            select new com.raota.domain.community.presentation.response.CommunityPostCardResponse(
                p.category,
                r.name,
                p.title,
                p.content,
                p.thumbnailUrl,
                m.nickname,
                p.createdAt,
                0L,
                0L
            )
            from PostEntity p
            join p.author m
            left join p.ramenShop r
            where m.id = :memberId
            order by p.createdAt desc
            """,
            countQuery = """
                    select count(p)
                    from PostEntity p
                    where p.author.id = :memberId
                    """)
    Page<com.raota.domain.community.presentation.response.CommunityPostCardResponse> findMyPosts(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.domain.community.presentation.response.CommunityCommentItemResponse(
                c.id,
                m.id,
                m.nickname,
                m.imageUrl,
                c.content,
                c.createdAt
            )
            from CommentEntity c
            join c.author m
            where m.id = :memberId
            order by c.createdAt desc
            """,
            countQuery = """
                    select count(c)
                    from CommentEntity c
                    where c.author.id = :memberId
                    """)
    Page<com.raota.domain.community.presentation.response.CommunityCommentItemResponse> findMyComments(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.domain.member.controller.response.VisitSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                CONCAT(CONCAT(r.address.city, ' '), r.address.district),
                count(p.id),
                max(p.uploadAt))
            from MemberProfile m
            left join RamenProofPicture p on p.memberProfile = m
            left join p.ramenShop r
            where m.id = :memberId
            group by r.id, r.name, r.imageUrl, r.address.city, r.address.district
            order by count(p.id) desc
            """,
            countQuery = """
                    select count(distinct r.id)
                    from MemberProfile m
                    left join RamenProofPicture p on p.memberProfile = m
                    left join p.ramenShop r
                    where m.id = :memberId
                    """)
    Page<VisitSummaryResponse> findMyVisitRestaurant(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.domain.member.controller.response.BookmarkSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                concat(r.address.city, concat(' ', r.address.district)),
                b.markingAt
            )
            from MemberProfile m
            join Bookmark b on b.memberProfile = m
            join b.ramenShop r
            where m.id = :memberId
            order by b.markingAt desc
            """,
            countQuery = """
                    select count(b)
                    from MemberProfile m
                    join Bookmark b on b.memberProfile = m
                    where m.id = :memberId
                    """)
    Page<BookmarkSummaryResponse> findMyBookmarks(@Param("memberId") Long memberId, Pageable pageable);

    @Query(value = """
    select new com.raota.domain.member.controller.response.PhotoSummaryResponse(
        p.id,
        p.imageUrl,
        p.imageName,
        p.ramenShop.id,
        p.ramenShop.name,
        p.uploadAt
    )
    from RamenProofPicture p
    where p.memberProfile.id = :memberId
""",
            countQuery = """
    select count(p)
    from RamenProofPicture p
    where p.memberProfile.id = :memberId
""")
    Page<PhotoSummaryResponse> findMyPhotos(@Param("memberId") Long memberId, Pageable pageable);
}
