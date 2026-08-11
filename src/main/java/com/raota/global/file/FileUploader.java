package com.raota.global.file;

import com.raota.global.presentation.file.response.PresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

    String upload(MultipartFile file, String dirName);

    default PresignedUrlResponse getPresignedUrl(String dirName, String extension) {
        return getPresignedUrl(dirName, extension, null);
    }

    PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType);

    String getAccessibleUrl(String filePath);

    void delete(String filePath);
}
