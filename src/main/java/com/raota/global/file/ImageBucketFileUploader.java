package com.raota.global.file;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@Profile("prod")
public class ImageBucketFileUploader implements FileUploader{

    private final S3Presigner s3Presigner; // 최신 s3 라이브러리에 포함되어 있음
    private final S3Client s3Client;

    @Value("${oci.storage.bucket}")
    private String bucketName;

    @Value("${oci.storage.namespace}")
    private String namespace;

    @Value("${oci.storage.region}")
    private String region;

    public ImageBucketFileUploader(S3Presigner s3Presigner, S3Client s3Client) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    @Override
    public String upload(MultipartFile file, String dirName) {
        String objectKey = createObjectKey(dirName, resolveExtension(file));
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 업로드에 실패했습니다.", exception);
        }

        return toImageUrl(objectKey);
    }

    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension) {
        String uniqueFilename = createObjectKey(dirName, extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFilename)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 5분 한정 티켓
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedRequest.url().toString();
        String imageUrl = toImageUrl(uniqueFilename);

        return new PresignedUrlResponse(uploadUrl, imageUrl);
    }

    @Override
    public void delete(String filePath) {
        // TODO: S3Client를 이용한 삭제 로직 구현
    }

    private String createObjectKey(String dirName, String extension) {
        return dirName + "/" + UUID.randomUUID() + extension;
    }

    private String toImageUrl(String objectKey) {
        return String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespace, bucketName, objectKey);
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null || extension.isBlank() ? ".png" : "." + extension;
    }
}
