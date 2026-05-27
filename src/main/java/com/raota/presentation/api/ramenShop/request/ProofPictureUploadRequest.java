package com.raota.presentation.api.ramenShop.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProofPictureUploadRequest {
    private String imageUrl;
    private String description;
    private String menuName;
}
