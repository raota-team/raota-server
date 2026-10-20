package com.raota.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.raota.global.file.cleanup.ImageObjectStore.StoredImage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class ImageBucketObjectStoreTest {

    private static final Instant CUTOFF = Instant.parse("2026-10-13T00:00:00Z");

    private final S3Client s3 = mock(S3Client.class);

    private final ImageBucketObjectStore store = new ImageBucketObjectStore(s3);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(store, "bucketName", "raota");
    }

    @Test
    void 잘린_목록은_마지막_키를_marker로_이어_받고_기준_시각_이전_객체만_CDN_주소로_돌려준다() {
        when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(
                ListObjectsResponse.builder()
                    .isTruncated(true)
                    .contents(object("v2/ramen-logs/a.jpg", CUTOFF.minusSeconds(1)),
                            object("v2/ramen-logs/b.jpg", CUTOFF))
                    .build(),
                ListObjectsResponse.builder()
                    .isTruncated(false)
                    .contents(object("v2/ramen-logs/c.jpg", CUTOFF.minusSeconds(60)))
                    .build());

        List<StoredImage> images = store.listOlderThan("v2/ramen-logs/", CUTOFF);

        assertThat(images).containsExactly(
                new StoredImage("v2/ramen-logs/a.jpg", "https://images.raota.net/v2/ramen-logs/a.jpg",
                        CUTOFF.minusSeconds(1)),
                new StoredImage("v2/ramen-logs/c.jpg", "https://images.raota.net/v2/ramen-logs/c.jpg",
                        CUTOFF.minusSeconds(60)));
        ArgumentCaptor<ListObjectsRequest> requests = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(s3, times(2)).listObjects(requests.capture());
        assertThat(requests.getAllValues()).extracting(ListObjectsRequest::bucket).containsOnly("raota");
        assertThat(requests.getAllValues()).extracting(ListObjectsRequest::prefix).containsOnly("v2/ramen-logs/");
        assertThat(requests.getAllValues()).extracting(ListObjectsRequest::marker)
            .containsExactly(null, "v2/ramen-logs/b.jpg");
    }

    @Test
    void v2_사진_객체가_아니면_지우지_않는다() {
        assertThatThrownBy(() -> store.delete("shops/a.jpg")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.delete("v2/ramen-logs/")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(s3);
    }

    private static S3Object object(String key, Instant lastModified) {
        return S3Object.builder().key(key).lastModified(lastModified).build();
    }

}
