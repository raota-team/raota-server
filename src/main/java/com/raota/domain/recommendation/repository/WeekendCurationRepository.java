package com.raota.domain.recommendation.repository;

import com.raota.domain.recommendation.model.WeekendCuration;
import java.util.Optional;

public interface WeekendCurationRepository {
    Optional<WeekendCuration> findByYearWeek(Integer yearWeek);
    Optional<WeekendCuration> findLatest();
    WeekendCuration save(WeekendCuration curation);
}
