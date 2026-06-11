package com.raota.infrastructure.persistence.recommendation;

import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.domain.recommendation.repository.WeekendCurationRepository;
import com.raota.infrastructure.persistence.recommendation.entity.WeekendCurationEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WeekendCurationRepositoryImpl implements WeekendCurationRepository {

    private final JpaWeekendCurationRepository jpaWeekendCurationRepository;

    @Override
    public Optional<WeekendCuration> findByYearWeek(Integer yearWeek) {
        return jpaWeekendCurationRepository.findByYearWeek(yearWeek)
                .map(WeekendCurationEntity::toDomain);
    }

    @Override
    public Optional<WeekendCuration> findLatest() {
        return jpaWeekendCurationRepository.findLatest()
                .map(WeekendCurationEntity::toDomain);
    }

    @Override
    public WeekendCuration save(WeekendCuration curation) {
        WeekendCurationEntity entity = WeekendCurationEntity.from(curation);
        return jpaWeekendCurationRepository.save(entity).toDomain();
    }
}
