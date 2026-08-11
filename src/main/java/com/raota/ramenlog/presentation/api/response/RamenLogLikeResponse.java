package com.raota.ramenlog.presentation.api.response;

public record RamenLogLikeResponse(
        boolean liked,
        long likeCount
) {
}
