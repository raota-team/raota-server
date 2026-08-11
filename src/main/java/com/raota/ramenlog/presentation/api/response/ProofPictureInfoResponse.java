package com.raota.ramenlog.presentation.api.response;
public record ProofPictureInfoResponse(
        Long photo_id,
        Boolean isSuccess,
        String image_url
){
}
