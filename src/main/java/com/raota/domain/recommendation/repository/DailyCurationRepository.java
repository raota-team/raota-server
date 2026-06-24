package com.raota.domain.recommendation.repository;

import com.raota.domain.recommendation.model.DailyCuration;
import java.util.Optional;

public interface DailyCurationRepository {
    Optional<DailyCuration> findByDateKey(Integer dateKey);
    Optional<DailyCuration> findLatest();
    DailyCuration save(DailyCuration curation);
}
