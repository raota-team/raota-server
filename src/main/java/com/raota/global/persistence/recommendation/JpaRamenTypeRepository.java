package com.raota.global.persistence.recommendation;

import com.raota.global.persistence.recommendation.entity.RamenTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRamenTypeRepository extends JpaRepository<RamenTypeEntity, Long> {
}
