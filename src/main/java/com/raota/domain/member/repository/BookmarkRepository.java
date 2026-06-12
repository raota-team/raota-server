package com.raota.domain.member.repository;

import com.raota.domain.member.model.Bookmark;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(Long memberProfileId, Long ramenShopId);

    Optional<Bookmark> findByMemberProfileIdAndRamenShopId(Long memberProfileId, Long ramenShopId);

    void deleteAllByMemberProfileId(Long memberProfileId);
}
