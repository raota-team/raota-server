package com.raota.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FIleController {

    private final FileUploader fileUploader;

    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String dirName,
            @RequestParam String extension
    ){
        // 1. Uploader에게 URL 발급 요청
        PresignedUrlResponse response = fileUploader.getPresignedUrl(dirName, extension);

        // 2. 발급된 URL 정보 반환
        return ResponseEntity.ok(response);
    }
}
