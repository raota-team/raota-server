package com.raota.domain.ramenShop.repository;

import com.raota.domain.ramenShop.dto.VoteResultsDto;
import com.raota.domain.ramenShop.model.MenuVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuVoteRepository extends JpaRepository<MenuVote,Long>{
    @Query("""
        SELECT new com.raota.domain.ramenShop.dto.VoteResultsDto(
            m.id,
            m.name,
            COUNT(v.id),
            null
        )
        FROM NormalMenu m
        LEFT JOIN MenuVote v ON v.normalMenu.id = m.id
        WHERE m.ramenShop.id = :shopId
        GROUP BY m.id, m.name
        ORDER BY COUNT(v.id) DESC
        """)
    List<VoteResultsDto> findMenuVoteCounts(@Param("shopId") Long shopId);
}
