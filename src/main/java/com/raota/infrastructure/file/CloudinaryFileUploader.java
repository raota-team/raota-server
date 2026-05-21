package com.raota.infrastructure.file;

import com.raota.presentation.api.file.response.PresignedUrlResponse;
import com.cloudinary.Cloudinary;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component("cloudinaryFileUploader")
@Profile("prod")
@RequiredArgsConstructor
public class CloudinaryFileUploader implements FileUploader {

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file, String dirName) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    Map.of("folder", dirName));
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            throw new IllegalArgumentException("Cloudinary 이미지 업로드 실패", e);
        }
    }

    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String uniqueId = java.util.UUID.randomUUID().toString();
        String normalizedExtension = (extension != null && extension.startsWith(".")) ? extension : "." + extension;
        
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("public_id", uniqueId); // 폴더명 제외하고 UUID만!
        params.put("timestamp", timestamp);
        params.put("folder", dirName);

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);
        
        Map<String, Object> uploadParams = new java.util.HashMap<>(params);
        uploadParams.put("api_key", cloudinary.config.apiKey);
        uploadParams.put("signature", signature);

        String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudinary.config.cloudName);
        String finalImageUrl = String.format("https://res.cloudinary.com/%s/image/upload/%s/%s", 
                cloudinary.config.cloudName, dirName, uniqueId + normalizedExtension);
        
        return new PresignedUrlResponse(uploadUrl, finalImageUrl, uploadParams);
    }

    @Override
    public String getAccessibleUrl(String filePath) {
        if (filePath != null && filePath.contains("res.cloudinary.com") && !filePath.contains("f_auto")) {
            return filePath.replace("/upload/", "/upload/f_auto,q_auto/");
        }
        return filePath;
    }

    @Override
    public void delete(String filePath) {
        try {
            String publicId = extractPublicId(filePath);
            cloudinary.uploader().destroy(publicId, Map.of());
        } catch (Exception e) {
        }
    }

    private String extractPublicId(String url) {
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
