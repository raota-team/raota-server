package com.raota.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

    String upload(MultipartFile file, String dirName);

    PresignedUrlResponse getPresignedUrl(String dirName, String extension);

    void delete(String filePath);
}
