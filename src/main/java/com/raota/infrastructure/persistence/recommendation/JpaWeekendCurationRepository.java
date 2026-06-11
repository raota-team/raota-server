package com.raota.infrastructure.persistence.recommendation;

import com.raota.infrastructure.persistence.recommendation.entity.WeekendCurationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaWeekendCurationRepository extends JpaRepository<WeekendCurationEntity, Long> {
    Optional<WeekendCurationEntity> findByYearWeek(Integer yearWeek);

    @Query("SELECT w FROM WeekendCurationEntity w ORDER BY w.yearWeek DESC LIMIT 1")
    Optional<WeekendCurationEntity> findLatest();
}
