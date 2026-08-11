package com.raota.agent.domain.recommendation.repository;

import com.raota.agent.domain.recommendation.model.DailyCuration;
import java.util.Optional;

public interface DailyCurationRepository {
    Optional<DailyCuration> findByDateKey(Integer dateKey);
    Optional<DailyCuration> findLatest();
    DailyCuration save(DailyCuration curation);
}
