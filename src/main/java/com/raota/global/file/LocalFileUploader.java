package com.raota.global.file;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("!prod")
public class LocalFileUploader implements FileUploader{
    @Override
    public String upload(MultipartFile file, String dirName) {
        String dummyFilename = dirName + "/" + UUID.randomUUID() + resolveExtension(file);
        return "https://mock.cdn.com/" + dummyFilename;
    }

    @Override
    public PresignedUrlResponse getPresignedUrl(String dirName, String extension) {
        // 로컬 테스트용 가짜 응답
        String dummyFilename = dirName + "/" + UUID.randomUUID() + extension;
        String mockUploadUrl = "/files/mock-upload-endpoint";
        String mockImageUrl = "https://mock.cdn.com/" + dummyFilename;

        return new PresignedUrlResponse(mockUploadUrl, mockImageUrl);
    }

    @Override
    public void delete(String filePath) {
        System.out.println("로컬 파일 삭제 모킹: " + filePath);
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null || extension.isBlank() ? ".png" : "." + extension;
    }
}
