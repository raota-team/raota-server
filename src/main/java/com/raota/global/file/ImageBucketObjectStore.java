package com.raota.global.file;

import com.raota.global.file.cleanup.ImageObjectStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * 운영 OCI 저장소의 고아 사진 정리용 목록·삭제. OCI S3 호환 API가 문서로 지원하는 ListObjects(v1)를 marker로 넘기고 객체는
 * 하나씩 DeleteObject로 지운다(ListObjectsV2는 지원 목록에 없다).
 */
@Component
@Profile("prod")
public class ImageBucketObjectStore implements ImageObjectStore {

    private static final int PAGE_SIZE = 1000;

    private static final String DELETABLE_ROOT = "v2/";

    private final S3Client s3Client;

    @Value("${oci.storage.bucket}")
    private String bucketName;

    public ImageBucketObjectStore(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public List<StoredImage> listOlderThan(String prefix, Instant cutoff) {
        List<StoredImage> images = new ArrayList<>();
        String marker = null;
        while (true) {
            ListObjectsRequest.Builder request = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(PAGE_SIZE);
            if (marker != null) {
                request.marker(marker);
            }
            ListObjectsResponse page = s3Client.listObjects(request.build());
            for (S3Object object : page.contents()) {
                if (object.lastModified() != null && object.lastModified().isBefore(cutoff)) {
                    images.add(new StoredImage(
                            object.key(),
                            ImageBucketFileUploader.CLOUDFLARE_IMAGE_DOMAIN + object.key(),
                            object.lastModified()));
                }
            }
            if (!Boolean.TRUE.equals(page.isTruncated()) || page.contents().isEmpty()) {
                return images;
            }
            // 구분자 없이 조회하면 NextMarker가 오지 않을 수 있어 마지막 키에서 이어간다.
            marker = page.contents().getLast().key();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || !key.startsWith(DELETABLE_ROOT) || key.endsWith("/")) {
            throw new IllegalArgumentException("v2 사진 객체만 지울 수 있습니다: " + key);
        }
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
    }
}
