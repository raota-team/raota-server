package com.raota.presentation.api.ramenlog.response;

public record RamenLogLikeResponse(
        boolean liked,
        long likeCount
) {
}
