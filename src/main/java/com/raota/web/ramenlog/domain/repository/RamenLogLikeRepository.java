package com.raota.web.ramenlog.domain.repository;

import com.raota.web.ramenlog.domain.model.RamenLogLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RamenLogLikeRepository extends JpaRepository<RamenLogLike, Long> {
    boolean existsByRamenLogIdAndMemberId(Long ramenLogId, Long memberId);
    Optional<RamenLogLike> findByRamenLogIdAndMemberId(Long ramenLogId, Long memberId);
    void deleteAllByRamenLogId(Long ramenLogId);
    void deleteAllByMemberId(Long memberId);
    void deleteAllByRamenLogAuthorId(Long memberId);
}
