package com.raota.global.presentation.file;

import com.raota.global.file.FileUploader;
import com.raota.global.file.UploadTicketLimitExceededException;
import com.raota.global.file.UploadTicketRateLimiter;
import com.raota.global.presentation.file.response.PresignedUrlResponse;
import com.raota.global.security.IdentifiedPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private final UploadTicketRateLimiter rateLimiter;

    /**
     * 클라이언트 직접 업로드를 위한 티켓(POST 업로드 서명 또는 Cloudinary Signature)을 발급한다. 회원마다 1시간에
     * {@link UploadTicketRateLimiter#FILES_PER_HOUR}장까지이고, 매장 사진을 올리는 관리자는 세지 않는다.
     * @param type 이미지 용도 (PROFILE, BACKGROUND, SHOP, COMMUNITY, PROOF)
     * @param extension 파일 확장자 (jpg, png 등)
     */
    @GetMapping("/upload-ticket")
    public ResponseEntity<PresignedUrlResponse> getUploadTicket(
            @AuthenticationPrincipal IdentifiedPrincipal member,
            @RequestParam String type,
            @RequestParam String extension,
            @RequestParam(required = false) String contentType) {
        String dirName = resolveDirectory(type);
        if (!isAdmin() && !rateLimiter.tryAcquire("v1", member.memberId(), 1)) {
            throw new UploadTicketLimitExceededException();
        }
        PresignedUrlResponse response = fileUploader.getPresignedUrl(dirName, extension, contentType);
        return ResponseEntity.ok(response);
    }

    private static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
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
