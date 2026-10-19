package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShop;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * 공개 상태의 매장만 외부 조회에 내보낸다. 숨김 매장은 관리자용 조회({@code findLive*},
 * {@code findByIdAndDeletedAtIsNull})로만 읽는다.
 */
public interface MobileShopRepository extends JpaRepository<MobileShop, Long> {

    Optional<MobileShop> findByIdAndPublishedTrueAndDeletedAtIsNull(Long id);

    List<MobileShop> findByPublishedTrueAndDeletedAtIsNull();

    Optional<MobileShop> findByIdAndDeletedAtIsNull(Long id);

    /** 영업시간·혜택·사진을 통째로 바꿀 때 같은 매장의 동시 교체를 줄 세운다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shop from MobileShop shop where shop.id = :id and shop.deletedAt is null")
    Optional<MobileShop> findLiveForUpdate(Long id);

    /** 관리자 목록. {@code keyword}는 이름·지점명·주소 부분 일치, {@code published}가 null이면 공개·숨김 모두다. */
    @Query("""
            select shop from MobileShop shop
            where shop.deletedAt is null
                and (:published is null or shop.published = :published)
                and (:keyword is null or shop.name like concat('%', :keyword, '%')
                    or shop.branchName like concat('%', :keyword, '%') or shop.address like concat('%', :keyword, '%'))
            """)
    Page<MobileShop> findLiveForAdmin(String keyword, Boolean published, Pageable pageable);

    @Modifying
    @Query("""
            update MobileShop shop set shop.logCount = shop.logCount + 1,
                shop.satisfactionScoreSum = shop.satisfactionScoreSum + :score,
                shop.scoredLogCount = shop.scoredLogCount + :scored
            where shop.id = :id
            """)
    void recordLogAdded(Long id, int score, int scored);

    @Modifying
    @Query("""
            update MobileShop shop set shop.logCount =
                    case when shop.logCount > :logs then shop.logCount - :logs else 0 end,
                shop.satisfactionScoreSum =
                    case when shop.satisfactionScoreSum > :score then shop.satisfactionScoreSum - :score else 0 end,
                shop.scoredLogCount =
                    case when shop.scoredLogCount > :scored then shop.scoredLogCount - :scored else 0 end
            where shop.id = :id
            """)
    void recordLogsRemoved(Long id, int logs, int score, int scored);

    @Modifying
    @Query("""
            update MobileShop shop set shop.satisfactionScoreSum =
                    case when shop.satisfactionScoreSum + :delta > 0 then shop.satisfactionScoreSum + :delta else 0 end,
                shop.scoredLogCount =
                    case when shop.scoredLogCount + :scoredDelta > 0 then shop.scoredLogCount + :scoredDelta else 0 end
            where shop.id = :id
            """)
    void recordSatisfactionChanged(Long id, int delta, int scoredDelta);

    @Modifying
    @Query("update MobileShop shop set shop.logCount = :logCount where shop.id = :id")
    void updateLogCount(Long id, int logCount);

    @Query("select shop.id from MobileShop shop")
    List<Long> findAllIds();

    /** 엔티티의 오래된 조회수를 저장하지 않고 DB에서 원자적으로 증가시킨다. */
    @Modifying
    @Query("update MobileShop shop set shop.viewCount = shop.viewCount + 1 where shop.id = :id")
    void incrementViewCount(Long id);

    /** 요약 생성에 시간이 걸리므로 엔티티를 저장하지 않고 요약 컬럼만 바꿔 그 사이 바뀐 집계 컬럼을 덮어쓰지 않는다. */
    @Modifying
    @Query(value = """
            UPDATE tb_v2_shop SET ai_review_summary = :summary, ai_summary_keywords = CAST(:keywordsJson AS JSON),
                ai_summary_generated_at = :generatedAt, updated_at = :generatedAt
            WHERE id = :id AND deleted_at IS NULL
            """, nativeQuery = true)
    int updateAiSummary(Long id, String summary, String keywordsJson, Instant generatedAt);

    /** 요약이 없는 공개 매장 ID를 {@code afterId} 다음부터 ID 순으로 읽는다. */
    @Query("""
            select shop.id from MobileShop shop
            where shop.published = true and shop.deletedAt is null and shop.aiSummaryGeneratedAt is null
                and shop.id > :afterId
            order by shop.id
            """)
    List<Long> findPublishedIdsWithoutAiSummaryAfter(Long afterId);

    /**
     * 추천해도 되는 매장(공개·미삭제·폐업과 휴업 제외) 중 라멘 종류가 {@code ramenType}인 매장, 종류가 없으면 공백을 뺀 태그에
     * {@code tagKeyword}가 들어간 매장에서 조회수 1위의 ID다.
     */
    @Query(value = """
            SELECT id FROM tb_v2_shop
            WHERE is_published = TRUE AND deleted_at IS NULL
                AND business_status NOT IN ('CLOSED', 'TEMPORARILY_CLOSED')
                AND ((:ramenType IS NOT NULL AND JSON_CONTAINS(ramen_types, JSON_QUOTE(:ramenType)))
                    OR (:ramenType IS NULL AND :tagKeyword IS NOT NULL
                        AND REPLACE(CAST(tags AS CHAR), ' ', '') LIKE CONCAT('%', :tagKeyword, '%')))
            ORDER BY view_count DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findMostViewedRecommendableId(String ramenType, String tagKeyword);

}
