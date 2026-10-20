package com.raota.global.file;

import com.raota.global.presentation.file.response.PresignedUrlResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component("imageBucketFileUploader")
@Profile("prod")
public class ImageBucketFileUploader implements FileUploader {

    static final String CLOUDFLARE_IMAGE_DOMAIN = "https://images.raota.net/";

    /** 사진 한 장 상한. 앱도 같은 값으로 미리 거른다. */
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);

    private static final Map<String, String> IMAGE_TYPES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg", "png",
            "image/png", "webp", "image/webp", "gif", "image/gif");

    private final PresignedPostSigner postSigner;

    private final S3Client s3Client;

    @Value("${oci.storage.bucket}")
    private String bucketName;

    @Value("${oci.storage.namespace}")
    private String namespace;

    @Value("${oci.storage.region}")
    private String region;

    public ImageBucketFileUploader(PresignedPostSigner postSigner, S3Client s3Client) {
        this.postSigner = postSigner;
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
        }
        catch (IOException exception) {
            throw new IllegalArgumentException("이미지 업로드에 실패했습니다.", exception);
        }

        return toImageUrl(objectKey);
    }

    /**
     * PUT 대신 POST 업로드 서명을 준다(#84). 저장소가 크기({@link #MAX_IMAGE_BYTES} 이하)와 서명한
     * Content-Type·객체 키를 직접 검사하므로, 티켓을 받은 뒤 큰 파일이나 이미지가 아닌 파일로 바꿔 올리면 거절된다. 클라이언트는
     * {@code upload_params}를 form 필드로 먼저 보내고 파일을 마지막 {@code file} 필드로 보낸다.
     */
    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType) {
        String imageType = imageType(extension, contentType);
        String objectKey = createObjectKey(dirName, extension);
        PresignedPostSigner.PresignedPost post = postSigner.sign(objectKey, imageType, MAX_IMAGE_BYTES,
                UPLOAD_VALIDITY);
        return new PresignedUrlResponse(post.url(), toImageUrl(objectKey), new LinkedHashMap<>(post.fields()));
    }

    /** 확장자가 이미지가 아니거나 요청한 Content-Type과 맞지 않으면 거절한다. Content-Type이 없으면 확장자로 정한다. */
    private static String imageType(String extension, String contentType) {
        String normalized = extension == null ? "" : extension.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        String expected = IMAGE_TYPES.get(normalized);
        if (expected == null || (StringUtils.hasText(contentType) && !expected.equals(contentType.strip()))) {
            throw new IllegalArgumentException("jpg·png·webp·gif 이미지만 올릴 수 있습니다.");
        }
        return expected;
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
        }
        catch (Exception exception) {
            return filePath;
        }
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null || extension.isBlank() ? ".png" : "." + extension;
    }

}
