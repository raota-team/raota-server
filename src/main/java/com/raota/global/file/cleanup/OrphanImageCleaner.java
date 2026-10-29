package com.raota.global.file.cleanup;

import com.raota.global.file.cleanup.ImageObjectStore.StoredImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 접두어마다 보관 기간이 지난 객체 중 어느 모듈도 참조하지 않는 것을 지운다. 보관 기간은 앱 재시도 대기열이 한참 뒤에 같은 사진 주소로 기록을 만들 수
 * 있어 둔다. 참조 확인이 실패한 접두어는 건너뛰고, 한 번에 시도하는 삭제 수는 {@code maxDeletes}를 넘지 않는다.
 */
@Slf4j
public class OrphanImageCleaner {

    static final String ALLOWED_ROOT = "v2/";

    private final ImageObjectStore imageObjectStore;

    private final List<StoredImageReferences> storedImageReferences;

    private final Duration retention;

    private final int maxDeletes;

    // 스프링 빈이 아니라 정리할 때마다 만드는 객체라서, 받은 기여자 목록을 복사해 두려고 생성자를 직접 쓴다.
    public OrphanImageCleaner(
            ImageObjectStore imageObjectStore,
            List<StoredImageReferences> storedImageReferences,
            Duration retention,
            int maxDeletes) {
        this.imageObjectStore = imageObjectStore;
        this.storedImageReferences = List.copyOf(storedImageReferences);
        this.retention = retention;
        this.maxDeletes = maxDeletes;
    }

    public List<PrefixResult> clean(Instant now) {
        Instant cutoff = now.minus(retention);
        int remaining = maxDeletes;
        List<PrefixResult> results = new ArrayList<>();
        for (StoredImageReferences contributor : storedImageReferences) {
            PrefixResult result = cleanPrefix(contributor, cutoff, remaining);
            remaining -= result.deleted() + result.failed();
            results.add(result);
        }
        return results;
    }

    private PrefixResult cleanPrefix(StoredImageReferences contributor, Instant cutoff, int limit) {
        String prefix = contributor.prefix();
        if (!allowed(prefix)) {
            log.warn("고아 사진 정리: 허용하지 않는 접두어라 건너뜀 prefix={}", prefix);
            return PrefixResult.skipped(prefix, Status.REJECTED_PREFIX, 0);
        }
        List<StoredImage> candidates;
        try {
            candidates = imageObjectStore.listOlderThan(prefix, cutoff).stream()
                    .filter(image -> isCandidate(image, prefix, cutoff))
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("고아 사진 정리: 목록 조회 실패로 건너뜀 prefix={}", prefix, exception);
            return PrefixResult.skipped(prefix, Status.LISTING_FAILED, 0);
        }
        if (candidates.isEmpty()) {
            return new PrefixResult(prefix, Status.DONE, 0, 0, 0, 0, 0);
        }
        Set<String> referenced;
        try {
            referenced = Set.copyOf(contributor.referencedUrls(
                    candidates.stream().map(StoredImage::url).collect(Collectors.toSet())));
        } catch (RuntimeException exception) {
            log.warn("고아 사진 정리: 참조 확인 실패로 건너뜀 prefix={}", prefix, exception);
            return PrefixResult.skipped(prefix, Status.REFERENCE_CHECK_FAILED, candidates.size());
        }
        List<StoredImage> orphans = candidates.stream()
                .filter(image -> !referenced.contains(image.url()))
                .toList();
        int deleted = 0;
        int failed = 0;
        for (StoredImage orphan : orphans) {
            if (deleted + failed >= limit) {
                break;
            }
            try {
                imageObjectStore.delete(orphan.key());
                deleted++;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("고아 사진 정리: 삭제 실패 key={}", orphan.key(), exception);
            }
        }
        int referencedCount = candidates.size() - orphans.size();
        int deferred = orphans.size() - deleted - failed;
        return new PrefixResult(prefix, Status.DONE, candidates.size(), referencedCount, deleted, failed, deferred);
    }

    private static boolean allowed(String prefix) {
        return prefix != null
                && prefix.startsWith(ALLOWED_ROOT)
                && prefix.endsWith("/")
                && prefix.length() > ALLOWED_ROOT.length()
                && !prefix.contains("..");
    }

    /** 저장소 구현이 무엇을 돌려주든 접두어 밖, 폴더 표시 객체, 보관 기간 안의 객체는 지우지 않는다. */
    private static boolean isCandidate(StoredImage image, String prefix, Instant cutoff) {
        return image.key() != null
                && image.key().startsWith(prefix)
                && !image.key().endsWith("/")
                && image.url() != null
                && image.lastModified() != null
                && image.lastModified().isBefore(cutoff);
    }

    public enum Status {
        DONE,
        REJECTED_PREFIX,
        LISTING_FAILED,
        REFERENCE_CHECK_FAILED
    }

    /**
     * @param candidates 보관 기간이 지난 객체 수
     * @param referenced 그중 DB가 참조하는 수
     * @param deferred 삭제 상한에 걸려 다음 실행으로 미룬 고아 수
     */
    public record PrefixResult(
            String prefix, Status status, int candidates, int referenced, int deleted, int failed, int deferred) {

        static PrefixResult skipped(String prefix, Status status, int candidates) {
            return new PrefixResult(prefix, status, candidates, 0, 0, 0, 0);
        }
    }
}
