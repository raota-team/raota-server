package com.raota.ramenlog.presentation.api.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProofPictureUploadRequest {
    private String imageUrl;
    private String description;
    private String menuName;
}
