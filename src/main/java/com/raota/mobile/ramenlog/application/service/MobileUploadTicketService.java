package com.raota.mobile.ramenlog.application.service;

import com.raota.global.file.FileUploader;
import com.raota.global.file.UploadTicketRateLimiter;
import com.raota.global.presentation.file.response.PresignedUrlResponse;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.command.MobileUploadTicketCommand;
import com.raota.mobile.ramenlog.application.result.MobileUploadTicketsResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사진 업로드 티켓. 운영 저장소는 POST 업로드 서명을 주고 크기(10MB)와 형식을 저장소가 검사한다(#84). 회원마다 1시간에
 * {@link UploadTicketRateLimiter#FILES_PER_HOUR}장까지 발급한다.
 */
@Service
@RequiredArgsConstructor
public class MobileUploadTicketService {

    private final FileUploader uploader;

    private final UploadTicketRateLimiter rateLimiter;

    public MobileUploadTicketsResult issue(Long userId, MobileUploadTicketCommand command) {
        if (command.purpose() == MobileUploadTicketCommand.Purpose.PROFILE
                && command.files().size() != 1) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "프로필 사진은 한 장만 업로드할 수 있습니다.");
        }
        String directory = switch (command.purpose()) {
            case PROFILE -> "v2/profiles";
            case RAMEN_LOG -> "v2/ramen-logs";
        };
        for (MobileUploadTicketCommand.File file : command.files()) {
            if (!validExtension(file.contentType(), file.extension())) {
                throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "사진 형식과 확장자를 확인해 주세요.");
            }
        }
        // 형식 검사를 통과한 요청만 센다.
        if (!rateLimiter.tryAcquire("v2", userId, command.files().size())) {
            throw new MobileException(
                    MobileErrorCode.RATE_LIMITED,
                    "사진은 1시간에 " + UploadTicketRateLimiter.FILES_PER_HOUR + "장까지 올릴 수 있습니다.");
        }
        List<MobileUploadTicketsResult.Upload> uploads = command.files().stream()
                .map(file -> upload(
                        uploader.getPresignedUrl(directory, "." + file.extension(), file.contentType()),
                        file.contentType()))
                .toList();
        return new MobileUploadTicketsResult(uploads);
    }

    /** 서명에 form 필드가 있으면 POST 업로드(운영 저장소), 없으면 PUT 업로드(로컬 개발)다. */
    private static MobileUploadTicketsResult.Upload upload(PresignedUrlResponse signed, String contentType) {
        if (signed.uploadParams() == null || signed.uploadParams().isEmpty()) {
            return new MobileUploadTicketsResult.Upload(
                    "PUT", signed.uploadUrl(), null, Map.of("Content-Type", contentType), signed.imgUrl());
        }
        Map<String, String> fields = new LinkedHashMap<>();
        signed.uploadParams().forEach((name, value) -> fields.put(name, String.valueOf(value)));
        return new MobileUploadTicketsResult.Upload("POST", signed.uploadUrl(), fields, null, signed.imgUrl());
    }

    private boolean validExtension(String contentType, String extension) {
        return switch (contentType) {
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            case "image/png" -> extension.equals("png");
            case "image/webp" -> extension.equals("webp");
            default -> false;
        };
    }
}
