package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileRamenLogImageRepository extends JpaRepository<MobileRamenLogImage, Long> {

    List<MobileRamenLogImage> findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(Collection<Long> logIds);

    void deleteByRamenLogId(Long logId);

}
