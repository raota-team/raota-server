package com.raota.presentation.api.file;

import com.raota.infrastructure.file.FileUploader;
import com.raota.presentation.api.file.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FIleController {

    private final FileUploader fileUploader;

    /**
     * 클라이언트 직접 업로드를 위한 티켓(Presigned URL 또는 Cloudinary Signature)을 발급한다.
     * @param type 이미지 용도 (PROFILE, BACKGROUND, SHOP, COMMUNITY, PROOF)
     * @param extension 파일 확장자 (jpg, png 등)
     */
    @GetMapping("/upload-ticket")
    public ResponseEntity<PresignedUrlResponse> getUploadTicket(
            @RequestParam String type,
            @RequestParam String extension,
            @RequestParam(required = false) String contentType
    ) {
        String dirName = resolveDirectory(type);
        PresignedUrlResponse response = fileUploader.getPresignedUrl(dirName, extension, contentType);
        return ResponseEntity.ok(response);
    }

    private String resolveDirectory(String type) {
        return switch (type.toUpperCase()) {
            case "PROFILE" -> "profiles";
            case "BACKGROUND" -> "backgrounds";
            case "SHOP", "RAMEN_SHOP", "RAMEN-SHOP" -> "shops";
            case "RAMEN_MENU", "RAMEN-MENU" -> "ramen-menu";
            case "COMMUNITY" -> "community";
            case "PROOF" -> "ramen-proof";
            default -> throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + type);
        };
    }

    @PutMapping("/mock-upload-endpoint")
    public ResponseEntity<Void> mockUpload() {
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
