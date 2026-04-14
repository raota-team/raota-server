package com.raota.global.file;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
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
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType) {
        String uniqueFilename = createObjectKey(dirName, extension);

        PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFilename);

        if (StringUtils.hasText(contentType)) {
            putObjectRequestBuilder.contentType(contentType);
        }

        PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

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
    public String getAccessibleUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return filePath;
        }

        String objectKey = extractObjectKey(filePath.trim());
        if (!StringUtils.hasText(objectKey)) {
            return filePath;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public void delete(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }

        // 이미지 URL에서 오브젝트 키(경로)를 추출하는 로직이 필요할 수 있으나, 
        // 여기서는 전달된 filePath가 키라고 가정하고 삭제를 진행합니다.
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(filePath));
    }

    private String createObjectKey(String dirName, String extension) {
        return dirName + "/" + UUID.randomUUID() + extension;
    }

    private String toImageUrl(String objectKey) {
        return String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespace, bucketName, objectKey);
    }

    private String extractObjectKey(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        if (!filePath.startsWith("http://") && !filePath.startsWith("https://")) {
            return filePath;
        }

        try {
            URI uri = URI.create(filePath);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }

            String objectPathPrefix = "/n/" + namespace + "/b/" + bucketName + "/o/";
            int objectPathIndex = path.indexOf(objectPathPrefix);
            if (objectPathIndex >= 0) {
                return path.substring(objectPathIndex + objectPathPrefix.length());
            }

            String compatPrefix = "/" + bucketName + "/";
            if (path.startsWith(compatPrefix)) {
                return path.substring(compatPrefix.length());
            }
            return null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null || extension.isBlank() ? ".png" : "." + extension;
    }
}
