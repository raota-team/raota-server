package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShop;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 공개 상태의 매장만 외부 조회에 내보낸다. */
public interface MobileShopRepository extends JpaRepository<MobileShop, Long> {

    Optional<MobileShop> findByIdAndPublishedTrueAndDeletedAtIsNull(Long id);

    List<MobileShop> findByPublishedTrueAndDeletedAtIsNull();

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

    @Query("""
            select shop.id from MobileShop shop
            where shop.published = true and shop.deletedAt is null and shop.aiSummaryGeneratedAt is null
            order by shop.id
            """)
    List<Long> findPublishedIdsWithoutAiSummary();

}
