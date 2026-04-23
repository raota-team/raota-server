package com.raota.global.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record PresignedUrlResponse(
        @JsonProperty("upload_url")
        String uploadUrl,
        
        @JsonProperty("img_url")
        String imgUrl,
        
        @JsonProperty("upload_params")
        Map<String, Object> uploadParams
) {
    public static PresignedUrlResponse of(String uploadUrl, String imgUrl) {
        return new PresignedUrlResponse(uploadUrl, imgUrl, null);
    }
}
