package com.raota.ramenlog.application.statistics;

import com.raota.ramenlog.domain.repository.RamenLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RamenLogStatisticsService {

    private final RamenLogRepository ramenLogRepository;

    @Transactional(readOnly = true)
    public long countActiveLogs() {
        return ramenLogRepository.countByIsDeletedFalse();
    }
}
