package com.raota.domain.recommendation.repository;

import com.raota.domain.recommendation.model.RamenType;
import java.util.List;
import java.util.Optional;

public interface RamenTypeRepository {
    Optional<RamenType> findById(Long id);
    List<RamenType> findAll();
    RamenType save(RamenType ramenType);
}
