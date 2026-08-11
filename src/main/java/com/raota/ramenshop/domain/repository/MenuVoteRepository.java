package com.raota.ramenshop.domain.repository;

import com.raota.ramenshop.presentation.response.VoteResultsDto;
import com.raota.ramenshop.domain.model.MenuVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuVoteRepository extends JpaRepository<MenuVote,Long>{

    @Query("SELECT COUNT(v) > 0 FROM MenuVote v WHERE v.memberProfile.id = :memberId AND v.ramenShop.id = :shopId AND v.isCancelled = false")
    boolean existsByMemberIdAndShopId(@Param("memberId") Long memberId, @Param("shopId") Long shopId);

    Optional<MenuVote> findByMemberProfileIdAndRamenShopId(Long memberId, Long shopId);

    Optional<MenuVote> findByMemberProfileIdAndRamenShopIdAndIsCancelledFalse(Long memberId, Long shopId);

    @Query("""
        SELECT new com.raota.ramenshop.presentation.response.VoteResultsDto(
            m.id,
            m.name,
            (SELECT COUNT(v.id) FROM MenuVote v WHERE v.normalMenu.id = m.id AND v.ramenShop.id = :shopId AND v.isCancelled = false),
            null,
            false
            )
        FROM NormalMenu m
        WHERE m.ramenShop.id = :shopId
        ORDER BY m.id ASC
        """)
    List<VoteResultsDto> findMenuVoteCounts(@Param("shopId") Long shopId);
}
