package com.raota.global.file.cleanup;

import com.raota.global.file.cleanup.OrphanImageCleaner.PrefixResult;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 앱이 올렸지만 어디에도 연결되지 않은 사진을 매일 지운다. 운영 저장소({@link ImageObjectStore})가 없는 프로필에서는 아무것도 하지
 * 않는다. 서버 여러 대 중 Redis 락을 먼저 잡은 한 곳만 실행한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.file.orphan-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class OrphanImageCleanupScheduler {

    static final String LOCK_KEY = "lock:v2:orphan-image-cleanup";

    /** 실행이 끝나도 풀지 않는다. 서버 시계가 조금 어긋나 늦게 깨어난 서버가 같은 날 다시 돌지 않게 TTL로만 놓는다. */
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    private final ObjectProvider<ImageObjectStore> stores;

    private final List<StoredImageReferences> references;

    private final RedisLockClient locks;

    private final OrphanImageCleanupProperties properties;

    private final Clock clock;

    public OrphanImageCleanupScheduler(ObjectProvider<ImageObjectStore> stores, List<StoredImageReferences> references,
            RedisLockClient locks, OrphanImageCleanupProperties properties, Clock clock) {
        this.stores = stores;
        this.references = references;
        this.locks = locks;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 10 5 * * *", zone = "Asia/Seoul")
    public void cleanOrphanImages() {
        ImageObjectStore store = stores.getIfAvailable();
        if (store == null || references.isEmpty()) {
            return;
        }
        try {
            if (locks.tryAcquire(LOCK_KEY, LOCK_TTL).isEmpty()) {
                log.info("고아 사진 정리: 다른 서버가 실행 중이라 건너뜀");
                return;
            }
        }
        catch (RedisLockUnavailableException exception) {
            log.warn("고아 사진 정리: Redis 락을 잡지 못해 건너뜀", exception);
            return;
        }
        List<PrefixResult> results = new OrphanImageCleaner(store, references, properties.retention(),
                properties.maxDeletes())
            .clean(clock.instant());
        for (PrefixResult result : results) {
            log.info("고아 사진 정리 prefix={} status={} 후보={} 참조={} 삭제={} 실패={} 보류={}", result.prefix(), result.status(),
                    result.candidates(), result.referenced(), result.deleted(), result.failed(), result.deferred());
        }
    }

}
