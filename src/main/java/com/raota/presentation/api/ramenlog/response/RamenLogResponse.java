package com.raota.presentation.api.ramenlog.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raota.domain.ramenlog.model.RamenLog;
import com.raota.domain.ramenlog.model.RevisitIntention;
import com.raota.global.file.FileUploader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RamenLogResponse(
        Long id,
        AuthorResponse author,
        ShopResponse shop,
        String menuName,
        String ramenType,
        String imageUrl,
        LocalDate visitedAt,
        LocalDateTime createdAt,
        String note,
        TasteNotesResponse tasteNotes,
        RevisitIntention revisit,
        long likeCount,
        boolean liked,
        @JsonProperty("public") boolean isPublic,
        boolean mine
) {
    public static RamenLogResponse from(RamenLog log, Long viewerId, boolean liked, FileUploader fileUploader) {
        return new RamenLogResponse(
                log.getId(),
                new AuthorResponse(
                        log.getAuthor().getId(),
                        log.getAuthor().getNickname(),
                        fileUploader.getAccessibleUrl(log.getAuthor().getImageUrl())
                ),
                new ShopResponse(
                        log.getRamenShop().getId(),
                        log.getRamenShop().getName(),
                        log.getRamenShop().getAddress().simpleAddress()
                ),
                log.getMenuName(),
                log.getRamenType(),
                fileUploader.getAccessibleUrl(log.getImageUrl()),
                log.getVisitedAt(),
                log.getCreatedAt(),
                log.getNote(),
                new TasteNotesResponse(
                        safe(log.getBrothNotes()),
                        safe(log.getNoodleNotes()),
                        safe(log.getSeasoningNotes()),
                        safe(log.getToppingNotes())
                ),
                log.getRevisit(),
                log.getLikeCount(),
                liked,
                log.isPublic(),
                viewerId != null && viewerId.equals(log.getAuthor().getId())
        );
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    public record AuthorResponse(Long id, String name, String imageUrl) {
    }

    public record ShopResponse(Long id, String name, String location) {
    }

    public record TasteNotesResponse(
            List<String> broth,
            List<String> noodle,
            List<String> seasoning,
            List<String> topping
    ) {
    }
}
