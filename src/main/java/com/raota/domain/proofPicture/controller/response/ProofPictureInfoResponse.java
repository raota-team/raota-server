package com.raota.domain.proofPicture.controller.response;
public record ProofPictureInfoResponse(
        Long photo_id,
        Boolean isSuccess,
        String image_url
){
}
