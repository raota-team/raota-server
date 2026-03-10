package com.raota.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

    String upload(MultipartFile file, String dirName);

    default PresignedUrlResponse getPresignedUrl(String dirName, String extension) {
        return getPresignedUrl(dirName, extension, null);
    }

    PresignedUrlResponse getPresignedUrl(String dirName, String extension, String contentType);

    void delete(String filePath);
}
