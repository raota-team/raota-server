package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShop;
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
                    case when shop.logCount > 0 then shop.logCount - 1 else 0 end,
                shop.satisfactionScoreSum =
                    case when shop.satisfactionScoreSum > :score then shop.satisfactionScoreSum - :score else 0 end,
                shop.scoredLogCount =
                    case when shop.scoredLogCount > :scored then shop.scoredLogCount - :scored else 0 end
            where shop.id = :id
            """)
    void recordLogRemoved(Long id, int score, int scored);

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

}
