package com.raota.web.ramenlog.presentation.api.response;

public record RamenLogLikeResponse(
        boolean liked,
        long likeCount
) {
}
