package com.raota.infrastructure.persistence.recommendation;

import com.raota.infrastructure.persistence.recommendation.entity.RamenTypeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRamenTypeRepository extends JpaRepository<RamenTypeEntity, Long> {
}
