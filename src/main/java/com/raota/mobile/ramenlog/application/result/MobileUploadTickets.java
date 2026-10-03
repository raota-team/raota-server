package com.raota.mobile.ramenlog.application.result;

import java.util.List;
import java.util.Map;

public record MobileUploadTickets(List<Upload> uploads) {

    public record Upload(String method, String url, Map<String, String> fields, Map<String, String> headers,
            String imageUrl) {
    }

}
