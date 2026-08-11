package com.raota.ramenshop.domain.repository;

import com.raota.ramenshop.domain.model.Bookmark;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(Long memberProfileId, Long ramenShopId);

    Optional<Bookmark> findByMemberProfileIdAndRamenShopId(Long memberProfileId, Long ramenShopId);

    void deleteAllByMemberProfileId(Long memberProfileId);
}
