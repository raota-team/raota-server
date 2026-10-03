package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogImageRepository extends JpaRepository<MobileRamenLogImage, Long> {

    List<MobileRamenLogImage> findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(Collection<Long> logIds);

    void deleteByRamenLogId(Long logId);

    @Modifying
    @Query("""
            delete from MobileRamenLogImage image
            where image.ramenLogId in (select log.id from MobileRamenLog log where log.userId = :userId)
            """)
    void deleteForUser(Long userId);

}
