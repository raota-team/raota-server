package com.raota.global.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("dev") // 개발 환경이나 특정 프로필에서 사용하도록 설정
@RequiredArgsConstructor
public class CloudinaryFileUploader implements FileUploader {

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file, String dirName) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", dirName));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new IllegalArgumentException("Cloudinary 이미지 업로드 실패", e);
        }
    }

    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType) {
        // Cloudinary는 클라이언트 직접 업로드를 위해 'upload_preset' 형식을 주로 사용합니다.
        // 여기서는 인터페이스 호환을 위해 mock 또는 에러 처리를 하거나, 
        // 실제 Cloudinary Signature 발급 로직으로 확장할 수 있습니다.
        throw new UnsupportedOperationException("Cloudinary는 Presigned URL 대신 Upload Preset 방식을 권장합니다.");
    }

    @Override
    public String getAccessibleUrl(String filePath) {
        // Cloudinary는 URL 자체가 이미 영구적이거나 변환을 포함할 수 있으므로 그대로 반환합니다.
        return filePath;
    }

    @Override
    public void delete(String filePath) {
        try {
            // filePath에서 public_id를 추출하는 로직이 필요합니다.
            String publicId = extractPublicId(filePath);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // 삭제 실패 로그 기록
        }
    }

    private String extractPublicId(String url) {
        // 예: https://res.cloudinary.com/demo/image/upload/v1234/folder/sample.jpg 
        // -> folder/sample (확장자 제외)
        if (url == null || !url.contains("upload/")) {
            return url;
        }
        String afterUpload = url.split("upload/")[1];
        String pathWithoutVersion = afterUpload.substring(afterUpload.indexOf("/") + 1);
        int lastDotIndex = pathWithoutVersion.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return pathWithoutVersion.substring(0, lastDotIndex);
        }
        return pathWithoutVersion;
    }
}
