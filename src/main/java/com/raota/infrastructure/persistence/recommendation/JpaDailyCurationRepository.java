package com.raota.infrastructure.persistence.recommendation;

import com.raota.infrastructure.persistence.recommendation.entity.DailyCurationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDailyCurationRepository extends JpaRepository<DailyCurationEntity, Long> {
    Optional<DailyCurationEntity> findByDateKey(Integer dateKey);
    Optional<DailyCurationEntity> findTopByOrderByDateKeyDesc();
}
