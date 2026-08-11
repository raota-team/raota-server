package com.raota.agent.infrastructure.persistence.recommendation;

import com.raota.agent.infrastructure.persistence.recommendation.entity.RamenTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRamenTypeRepository extends JpaRepository<RamenTypeEntity, Long> {
}
