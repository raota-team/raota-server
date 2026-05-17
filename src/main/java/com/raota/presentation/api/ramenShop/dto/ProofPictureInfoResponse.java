package com.raota.presentation.api.ramenShop.dto;
public record ProofPictureInfoResponse(
        Long photo_id,
        Boolean isSuccess,
        String image_url
){
}
