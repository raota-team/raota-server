package com.raota.infrastructure.file;

import com.raota.presentation.api.file.dto.PresignedUrlResponse;
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

@Component("imageBucketFileUploader")
@Profile("prod")
public class ImageBucketFileUploader implements FileUploader {

    private static final String CLOUDFLARE_IMAGE_DOMAIN = "https://images.raota.net/";

    private final S3Presigner s3Presigner;
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

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFilename)
                .contentType(StringUtils.hasText(contentType) ? contentType : null)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedRequest.url().toString();
        String imageUrl = toImageUrl(uniqueFilename);

        return PresignedUrlResponse.of(uploadUrl, imageUrl);
    }

    @Override
    public String getAccessibleUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return filePath;
        }

        if (filePath.startsWith(CLOUDFLARE_IMAGE_DOMAIN)) {
            return filePath;
        }

        String objectKey = extractObjectKey(filePath.trim());
        if (StringUtils.hasText(objectKey)) {
            return CLOUDFLARE_IMAGE_DOMAIN + objectKey;
        }

        return filePath;
    }

    @Override
    public void delete(String filePath) {
        String objectKey = extractObjectKey(filePath);
        if (StringUtils.hasText(objectKey)) {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey));
        }
    }

    private String createObjectKey(String dirName, String extension) {
        return dirName + "/" + UUID.randomUUID() + extension;
    }

    private String toImageUrl(String objectKey) {
        return CLOUDFLARE_IMAGE_DOMAIN + objectKey;
    }

    private String extractObjectKey(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        if (filePath.startsWith(CLOUDFLARE_IMAGE_DOMAIN)) {
            return filePath.substring(CLOUDFLARE_IMAGE_DOMAIN.length());
        }

        try {
            URI uri = URI.create(filePath);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return filePath;
            }

            String objectPathPrefix = "/n/" + namespace + "/b/" + bucketName + "/o/";
            int objectPathIndex = path.indexOf(objectPathPrefix);
            if (objectPathIndex >= 0) {
                return path.substring(objectPathIndex + objectPathPrefix.length());
            }

            return filePath;
        } catch (Exception exception) {
            return filePath;
        }
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null || extension.isBlank() ? ".png" : "." + extension;
    }
}
