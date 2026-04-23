package com.raota.domain.ramenShop.repository;

import com.raota.domain.ramenShop.dto.VoteResultsDto;
import com.raota.domain.ramenShop.model.MenuVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuVoteRepository extends JpaRepository<MenuVote,Long>{

    @Query("SELECT COUNT(v) > 0 FROM MenuVote v WHERE v.memberProfile.id = :memberId AND v.ramenShop.id = :shopId")
    boolean existsByMemberIdAndShopId(@Param("memberId") Long memberId, @Param("shopId") Long shopId);

    @Query("""
        SELECT new com.raota.domain.ramenShop.dto.VoteResultsDto(
            m.id,
            m.name,
            (SELECT COUNT(v.id) FROM MenuVote v WHERE v.normalMenu.id = m.id AND v.ramenShop.id = :shopId),
            null
        )
        FROM NormalMenu m
        WHERE m.ramenShop.id = :shopId
        ORDER BY m.id ASC
        """)
    List<VoteResultsDto> findMenuVoteCounts(@Param("shopId") Long shopId);
}
