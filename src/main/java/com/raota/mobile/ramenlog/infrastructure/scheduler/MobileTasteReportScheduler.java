package com.raota.mobile.ramenlog.infrastructure.scheduler;

import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockUnavailableException;
import com.raota.mobile.ramenlog.application.service.MobileTasteReportService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 락을 못 잡으면 건너뛴다. 빠진 뽑기는 알림 때와 리포트 조회 때 다시 채운다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileTasteReportScheduler {

    static final String DRAW_LOCK_KEY = "lock:v2:taste-report-draw";

    static final String NOTIFY_LOCK_KEY = "lock:v2:taste-report-notify";

    /** 실행이 끝나도 풀지 않아 1시간 동안 다시 실행되지 않는다. */
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MobileTasteReportService reports;

    private final RedisLockClient locks;

    private final Clock clock;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void drawMonthlyReports() {
        if (!acquire(DRAW_LOCK_KEY, "매달 취향 리포트 뽑기")) {
            return;
        }
        int drawn = reports.drawMonthlyForAll(today());
        log.info("매달 취향 리포트 {}개 뽑음", drawn);
    }

    @Scheduled(cron = "0 0 10 1 * *", zone = "Asia/Seoul")
    public void notifyMonthlyReports() {
        if (!acquire(NOTIFY_LOCK_KEY, "매달 취향 리포트 알림")) {
            return;
        }
        int sent = reports.notifyMonthly(today());
        log.info("매달 취향 리포트 알림 {}개 기기에 보냄", sent);
    }

    private boolean acquire(String key, String job) {
        try {
            if (locks.tryAcquire(key, LOCK_TTL).isEmpty()) {
                log.info("{}: 다른 서버가 실행 중이라 건너뜀", job);
                return false;
            }
        } catch (RedisLockUnavailableException exception) {
            log.warn("{}: Redis 락을 잡지 못해 건너뜀", job, exception);
            return false;
        }
        return true;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SEOUL));
    }
}
