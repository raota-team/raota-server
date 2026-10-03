package com.raota.mobile.ramenlog.application.service;

import com.raota.global.file.FileUploader;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.command.MobileUploadTicketCommand;
import com.raota.mobile.ramenlog.application.result.MobileUploadTickets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MobileUploadTicketService {

    private final FileUploader uploader;

    public MobileUploadTickets issue(MobileUploadTicketCommand command) {
        if (command.purpose() == MobileUploadTicketCommand.Purpose.PROFILE && command.files().size() != 1) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "프로필 사진은 한 장만 업로드할 수 있습니다.");
        }
        String directory = command.purpose() == MobileUploadTicketCommand.Purpose.PROFILE ? "v2/profiles"
                : "v2/ramen-logs";
        List<MobileUploadTickets.Upload> uploads = command.files().stream().map(file -> {
            String extension = file.extension();
            if (!validExtension(file.contentType(), extension)) {
                throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "사진 형식과 확장자를 확인해 주세요.");
            }
            var signed = uploader.getPresignedUrl(directory, "." + extension, file.contentType());
            return new MobileUploadTickets.Upload("PUT", signed.uploadUrl(), null,
                    Map.of("Content-Type", file.contentType()), signed.imgUrl());
        }).toList();
        return new MobileUploadTickets(uploads);
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
