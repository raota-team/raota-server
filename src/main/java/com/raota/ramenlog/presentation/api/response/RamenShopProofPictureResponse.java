package com.raota.ramenlog.presentation.api.response;

import java.time.LocalDateTime;

public record RamenShopProofPictureResponse(Long photo_id,
                                            Long uploaderId,
                                            String image_url,
                                            String uploader_nickname,
                                            String oneLineComment,
                                            String menuName,
                                            LocalDateTime uploaded_at) {
}
