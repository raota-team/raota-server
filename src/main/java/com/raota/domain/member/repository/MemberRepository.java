package com.raota.domain.member.repository;

import com.raota.presentation.api.member.response.BookmarkSummaryResponse;
import com.raota.presentation.api.member.response.MyProfileResponse;
import com.raota.presentation.api.member.response.PhotoSummaryResponse;
import com.raota.presentation.api.member.response.VisitSummaryResponse;
import com.raota.domain.member.model.MemberProfile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberProfile, Long> {
    Optional<MemberProfile> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Query("""
    select m
    from MemberProfile m
    where m.deletedAt is not null
      and m.deletedAt <= :cutoff
""")
    List<MemberProfile> findSoftDeletedMembersDueForPurge(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
    select new com.raota.presentation.api.member.response.MyProfileResponse(
        m.id,
        m.nickname,
        m.email,
        m.imageUrl,
        m.backgroundImageUrl,
        m.bio,
        new com.raota.presentation.api.member.response.UserStatsDto(
            (select count(distinct p.ramenShop.id) from RamenLog p where p.author.id = m.id and p.isDeleted = false),
            (select count(p) from RamenLog p where p.author.id = m.id and p.isDeleted = false),
            (select count(l) from RamenLog l where l.author.id = m.id and l.isDeleted = false),
            (select count(b) from Bookmark b where b.memberProfile.id = m.id and b.isDeleted = false),
            (select count(p) from PostEntity p where p.author.id = m.id and p.isDeleted = false),
            (select count(c) from CommentEntity c where c.member.id = m.id and c.isDeleted = false and c.post.isDeleted = false)
        ),
        new com.raota.presentation.api.member.response.ActivityVisibilityResponse(
            m.activityVisibility.logsPublic,
            m.activityVisibility.visitsPublic,
            m.activityVisibility.postsPublic,
            m.activityVisibility.commentsPublic
        )
    )
    from MemberProfile m
    where m.id = :id
      and m.deletedAt is null
""")
    MyProfileResponse findMemberDetailInfo(@Param("id") Long id);

    @Query(value = """
            select distinct m.*
            from tb_member_profile m
            left join tb_social_account sa on sa.member_id = m.id
            where (:keywordBlank = true
                   or lower(m.nickname) like :keyword
                   or lower(coalesce(m.email, '')) like :keyword
                   or (:keywordMemberId is not null and m.id = :keywordMemberId))
              and (:registrationCompleted is null or m.is_registration_completed = :registrationCompleted)
              and (:deleted is null
                   or (:deleted = true and m.deleted_at is not null)
                   or (:deleted = false and m.deleted_at is null))
              and (:provider is null or sa.provider = :provider)
              and (:emailPresent is null
                   or (:emailPresent = true and m.email is not null and m.email <> '')
                   or (:emailPresent = false and (m.email is null or m.email = '')))
            order by m.created_at desc, m.id desc
            """,
            countQuery = """
                    select count(distinct m.id)
                    from tb_member_profile m
                    left join tb_social_account sa on sa.member_id = m.id
                    where (:keywordBlank = true
                           or lower(m.nickname) like :keyword
                           or lower(coalesce(m.email, '')) like :keyword
                           or (:keywordMemberId is not null and m.id = :keywordMemberId))
                      and (:registrationCompleted is null or m.is_registration_completed = :registrationCompleted)
                      and (:deleted is null
                           or (:deleted = true and m.deleted_at is not null)
                           or (:deleted = false and m.deleted_at is null))
                      and (:provider is null or sa.provider = :provider)
                      and (:emailPresent is null
                           or (:emailPresent = true and m.email is not null and m.email <> '')
                           or (:emailPresent = false and (m.email is null or m.email = '')))
                    """,
            nativeQuery = true)
    Page<MemberProfile> findAdminUsers(
            @Param("keywordBlank") boolean keywordBlank,
            @Param("keyword") String keyword,
            @Param("keywordMemberId") Long keywordMemberId,
            @Param("registrationCompleted") Boolean registrationCompleted,
            @Param("deleted") Boolean deleted,
            @Param("provider") String provider,
            @Param("emailPresent") Boolean emailPresent,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.community.response.CommunityPostCardResponse(
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
                (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                p.viewCount
            )
            from PostEntity p
            join p.author m
            left join p.ramenShop r
            where m.id = :memberId and m.deletedAt is null and p.isDeleted = false
            order by p.createdAt desc
            """,
            countQuery = """
                    select count(p)
                    from PostEntity p
                    where p.author.id = :memberId and p.author.deletedAt is null and p.isDeleted = false
                    """)
    Page<com.raota.presentation.api.community.response.CommunityPostCardResponse> findMyPosts(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.community.response.CommunityCommentItemResponse(
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
                and m.deletedAt is null
                and c.isDeleted = false
                and c.post.isDeleted = false
            order by c.createdAt desc
            """,
            countQuery = """
                    select count(c)
                    from CommentEntity c
                    where c.member.id = :memberId
                        and c.member.deletedAt is null
                        and c.isDeleted = false
                        and c.post.isDeleted = false
                    """)
    Page<com.raota.presentation.api.community.response.CommunityCommentItemResponse> findMyComments(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.member.response.VisitSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                CONCAT(CONCAT(r.address.city, ' '), r.address.district),
                count(p.id),
                max(p.createdAt))
            from MemberProfile m
            join RamenLog p on p.author = m
            join p.ramenShop r
            where m.id = :memberId and m.deletedAt is null and p.isDeleted = false
            group by r.id, r.name, r.imageUrl, r.address.city, r.address.district
            having count(p.id) > 0
            order by count(p.id) desc
            """,
            countQuery = """
                    select count(distinct r.id)
                    from MemberProfile m
                    join RamenLog p on p.author = m
                    join p.ramenShop r
                    where m.id = :memberId and m.deletedAt is null and p.isDeleted = false
                    """)
    Page<VisitSummaryResponse> findMyVisitRestaurant(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query(value = """
            select new com.raota.presentation.api.member.response.BookmarkSummaryResponse(
                r.id,
                r.name,
                r.imageUrl,
                concat(r.address.city, concat(' ', r.address.district)),
                b.markingAt
            )
            from MemberProfile m
            join Bookmark b on b.memberProfile = m
            join b.ramenShop r
            where m.id = :memberId and m.deletedAt is null and b.isDeleted = false
            order by b.markingAt desc
            """,
            countQuery = """
                    select count(b)
                    from MemberProfile m
                    join Bookmark b on b.memberProfile = m
                    where m.id = :memberId and m.deletedAt is null and b.isDeleted = false
                    """)
    Page<BookmarkSummaryResponse> findMyBookmarks(@Param("memberId") Long memberId, Pageable pageable);

    @Query(value = """
    select new com.raota.presentation.api.member.response.PhotoSummaryResponse(
        p.id,
        p.imageUrl,
        p.menuName,
        p.note,
        p.ramenShop.id,
        p.ramenShop.name,
        p.createdAt
    )
    from RamenLog p
    where p.author.id = :memberId
      and p.author.deletedAt is null
      and p.isDeleted = false
""",
            countQuery = """
    select count(p)
    from RamenLog p
    where p.author.id = :memberId
      and p.author.deletedAt is null
      and p.isDeleted = false
""")
    Page<PhotoSummaryResponse> findMyPhotos(@Param("memberId") Long memberId, Pageable pageable);
}
