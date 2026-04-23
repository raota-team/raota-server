package com.raota.global.file;

import java.util.Map;

public record PresignedUrlResponse(
        String uploadUrl,
        String imgUrl,
        Map<String, Object> uploadParams
) {
    public static PresignedUrlResponse of(String uploadUrl, String imgUrl) {
        return new PresignedUrlResponse(uploadUrl, imgUrl, null);
    }
}
