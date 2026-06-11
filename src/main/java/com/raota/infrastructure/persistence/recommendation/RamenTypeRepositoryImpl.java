package com.raota.infrastructure.persistence.recommendation;

import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.infrastructure.persistence.recommendation.entity.RamenTypeEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RamenTypeRepositoryImpl implements RamenTypeRepository {

    private final JpaRamenTypeRepository jpaRamenTypeRepository;

    @Override
    public Optional<RamenType> findById(Long id) {
        return jpaRamenTypeRepository.findById(id).map(RamenTypeEntity::toDomain);
    }

    @Override
    public List<RamenType> findAll() {
        return jpaRamenTypeRepository.findAll().stream()
                .map(RamenTypeEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RamenType save(RamenType ramenType) {
        RamenTypeEntity entity = RamenTypeEntity.from(ramenType);
        return jpaRamenTypeRepository.save(entity).toDomain();
    }
}
