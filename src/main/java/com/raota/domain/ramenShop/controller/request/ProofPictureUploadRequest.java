package com.raota.domain.ramenShop.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProofPictureUploadRequest {
    private String imageUrl;
    private String imageName;
    private String description;
}
