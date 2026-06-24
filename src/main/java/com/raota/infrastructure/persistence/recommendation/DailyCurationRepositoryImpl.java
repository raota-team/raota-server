package com.raota.infrastructure.persistence.recommendation;

import com.raota.domain.recommendation.model.DailyCuration;
import com.raota.domain.recommendation.repository.DailyCurationRepository;
import com.raota.infrastructure.persistence.recommendation.entity.DailyCurationEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DailyCurationRepositoryImpl implements DailyCurationRepository {

    private final JpaDailyCurationRepository jpaDailyCurationRepository;

    @Override
    public Optional<DailyCuration> findByDateKey(Integer dateKey) {
        return jpaDailyCurationRepository.findByDateKey(dateKey)
                .map(DailyCurationEntity::toDomain);
    }

    @Override
    public Optional<DailyCuration> findLatest() {
        return jpaDailyCurationRepository.findTopByOrderByDateKeyDesc()
                .map(DailyCurationEntity::toDomain);
    }

    @Override
    public DailyCuration save(DailyCuration curation) {
        DailyCurationEntity entity = DailyCurationEntity.from(curation);
        return jpaDailyCurationRepository.save(entity).toDomain();
    }
}
