package com.raota.global.file;

import com.raota.presentation.api.file.response.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 용도에 따라 다른 업로더(OCI vs Cloudinary)를 선택하여 위임하는 브로커 클래스.
 * @Primary 설정을 통해 시스템 전체에서 기본 FileUploader로 사용됨.
 */
@Primary
@Component
@Profile("prod")
public class RoutingFileUploader implements FileUploader {

    private final FileUploader ociUploader;
    private final FileUploader cloudinaryUploader;

    private static final String RAMEN_PROOF_DIR = "ramen-proof";

    public RoutingFileUploader(
            @Qualifier("imageBucketFileUploader") FileUploader ociUploader,
            @Qualifier("cloudinaryFileUploader") FileUploader cloudinaryUploader
    ) {
        this.ociUploader = ociUploader;
        this.cloudinaryUploader = cloudinaryUploader;
    }

    @Override
    public String upload(MultipartFile file, String dirName) {
        return getUploader(dirName).upload(file, dirName);
    }

    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType) {
        return getUploader(dirName).getPresignedUrl(dirName, extension, contentType);
    }

    @Override
    public String getAccessibleUrl(String filePath) {
        if (filePath != null && filePath.contains("cloudinary")) {
            return cloudinaryUploader.getAccessibleUrl(filePath);
        }
        return ociUploader.getAccessibleUrl(filePath);
    }

    @Override
    public void delete(String filePath) {
        if (filePath != null && filePath.contains("cloudinary")) {
            cloudinaryUploader.delete(filePath);
        } else {
            ociUploader.delete(filePath);
        }
    }

    private FileUploader getUploader(String dirName) {
        // 인증샷(ramen-proof)은 Cloudinary 사용, 그 외(posts, shop, profile 등)는 OCI 사용
        if (RAMEN_PROOF_DIR.equals(dirName)) {
            return cloudinaryUploader;
        }
        return ociUploader;
    }
}
