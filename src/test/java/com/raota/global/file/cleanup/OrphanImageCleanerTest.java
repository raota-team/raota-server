package com.raota.global.file.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.file.cleanup.ImageObjectStore.StoredImage;
import com.raota.global.file.cleanup.OrphanImageCleaner.PrefixResult;
import com.raota.global.file.cleanup.OrphanImageCleaner.Status;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrphanImageCleanerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T20:10:00Z");

    private static final Duration RETENTION = Duration.ofDays(7);

    private static final String CDN = "https://images.raota.net/";

    private final FakeStore store = new FakeStore();

    @Test
    void 보관_기간을_넘긴_객체만_지우고_경계_시각의_객체는_남긴다() {
        store.put("v2/ramen-logs/old.jpg", NOW.minus(RETENTION).minusSeconds(1));
        store.put("v2/ramen-logs/boundary.jpg", NOW.minus(RETENTION));
        store.put("v2/ramen-logs/recent.jpg", NOW.minus(Duration.ofDays(1)));

        List<PrefixResult> results = cleaner(500, references("v2/ramen-logs/")).clean(NOW);

        assertThat(store.deleted).containsExactly("v2/ramen-logs/old.jpg");
        assertThat(results).containsExactly(new PrefixResult("v2/ramen-logs/", Status.DONE, 1, 0, 1, 0, 0));
    }

    @Test
    void 참조되는_사진은_남기고_참조_없는_사진만_지운다() {
        store.put("v2/profiles/used.png", old());
        store.put("v2/profiles/orphan.png", old());

        List<PrefixResult> results = cleaner(500, references("v2/profiles/", CDN + "v2/profiles/used.png"))
                .clean(NOW);

        assertThat(store.deleted).containsExactly("v2/profiles/orphan.png");
        assertThat(results).containsExactly(new PrefixResult("v2/profiles/", Status.DONE, 2, 1, 1, 0, 0));
    }

    @Test
    void 참조_확인이_실패하면_그_접두어는_하나도_지우지_않고_다음_접두어는_정리한다() {
        store.put("v2/ramen-logs/a.jpg", old());
        store.put("v2/profiles/b.png", old());
        StoredImageReferences failing = new StoredImageReferences() {
            @Override
            public String prefix() {
                return "v2/ramen-logs/";
            }

            @Override
            public Set<String> referencedUrls(Collection<String> urls) {
                throw new IllegalStateException("DB down");
            }
        };

        List<PrefixResult> results =
                cleaner(500, failing, references("v2/profiles/")).clean(NOW);

        assertThat(store.deleted).containsExactly("v2/profiles/b.png");
        assertThat(results.getFirst())
                .isEqualTo(PrefixResult.skipped("v2/ramen-logs/", Status.REFERENCE_CHECK_FAILED, 1));
    }

    @Test
    void 목록_조회가_실패하면_그_접두어는_건너뛴다() {
        store.failListing = true;

        List<PrefixResult> results = cleaner(500, references("v2/ramen-logs/")).clean(NOW);

        assertThat(store.deleted).isEmpty();
        assertThat(results).containsExactly(PrefixResult.skipped("v2/ramen-logs/", Status.LISTING_FAILED, 0));
    }

    @Test
    void 한_번에_지우는_수는_접두어를_합쳐_상한을_넘지_않고_나머지는_보류한다() {
        for (int index = 0; index < 3; index++) {
            store.put("v2/ramen-logs/" + index + ".jpg", old());
            store.put("v2/profiles/" + index + ".png", old());
        }

        List<PrefixResult> results = cleaner(4, references("v2/ramen-logs/"), references("v2/profiles/"))
                .clean(NOW);

        assertThat(store.deleted).hasSize(4);
        assertThat(results)
                .containsExactly(
                        new PrefixResult("v2/ramen-logs/", Status.DONE, 3, 0, 3, 0, 0),
                        new PrefixResult("v2/profiles/", Status.DONE, 3, 0, 1, 0, 2));
    }

    @Test
    void 삭제_실패도_상한에_세고_나머지는_계속_지운다() {
        store.put("v2/ramen-logs/a.jpg", old());
        store.put("v2/ramen-logs/b.jpg", old());
        store.put("v2/ramen-logs/c.jpg", old());
        store.failDeleteKey = "v2/ramen-logs/a.jpg";

        List<PrefixResult> results = cleaner(2, references("v2/ramen-logs/")).clean(NOW);

        assertThat(store.deleted).containsExactly("v2/ramen-logs/b.jpg");
        assertThat(results).containsExactly(new PrefixResult("v2/ramen-logs/", Status.DONE, 3, 0, 1, 1, 1));
    }

    @Test
    void v2로_시작하지_않는_접두어는_목록도_조회하지_않고_거부한다() {
        store.put("ramenshop/a.jpg", old());
        store.put("v2/a.jpg", old());

        List<PrefixResult> results = cleaner(
                        500,
                        references("ramenshop/"),
                        references(""),
                        references("v2/"),
                        references("v2/../shops/"),
                        references("v2/profiles"))
                .clean(NOW);

        assertThat(store.deleted).isEmpty();
        assertThat(store.listedPrefixes).isEmpty();
        assertThat(results).extracting(PrefixResult::status).containsOnly(Status.REJECTED_PREFIX);
    }

    @Test
    void 저장소가_접두어_밖이나_폴더_표시_객체를_돌려줘도_지우지_않는다() {
        store.put("v2/profiles/x.png", old());
        store.put("v2/ramen-logs/", old());
        store.ignorePrefix = true;

        cleaner(500, references("v2/ramen-logs/")).clean(NOW);

        assertThat(store.deleted).isEmpty();
    }

    private OrphanImageCleaner cleaner(int maxDeletes, StoredImageReferences... references) {
        return new OrphanImageCleaner(store, List.of(references), RETENTION, maxDeletes);
    }

    private static Instant old() {
        return NOW.minus(Duration.ofDays(30));
    }

    private static StoredImageReferences references(String prefix, String... referencedUrls) {
        return new StoredImageReferences() {
            @Override
            public String prefix() {
                return prefix;
            }

            @Override
            public Set<String> referencedUrls(Collection<String> urls) {
                Set<String> referenced = new HashSet<>(urls);
                referenced.retainAll(Set.of(referencedUrls));
                return referenced;
            }
        };
    }

    /** 보관 기간은 거르지 않고 돌려줘 정리기가 경계를 직접 지키는지 본다. */
    private static final class FakeStore implements ImageObjectStore {

        private final List<StoredImage> objects = new ArrayList<>();

        private final List<String> deleted = new ArrayList<>();

        private final List<String> listedPrefixes = new ArrayList<>();

        private boolean failListing;

        private boolean ignorePrefix;

        private String failDeleteKey;

        void put(String key, Instant lastModified) {
            objects.add(new StoredImage(key, CDN + key, lastModified));
        }

        @Override
        public List<StoredImage> listOlderThan(String prefix, Instant cutoff) {
            listedPrefixes.add(prefix);
            if (failListing) {
                throw new IllegalStateException("storage down");
            }
            return objects.stream()
                    .filter(object -> ignorePrefix || object.key().startsWith(prefix))
                    .toList();
        }

        @Override
        public void delete(String key) {
            if (key.equals(failDeleteKey)) {
                throw new IllegalStateException("delete failed");
            }
            deleted.add(key);
        }
    }
}
