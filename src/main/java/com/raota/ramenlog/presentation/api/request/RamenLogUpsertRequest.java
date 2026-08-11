package com.raota.ramenlog.presentation.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raota.ramenlog.domain.model.RevisitIntention;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record RamenLogUpsertRequest(
        @NotNull Long shopId,
        @NotBlank @Size(max = 255) String menuName,
        @NotBlank @Size(max = 50) String ramenType,
        @NotBlank @Size(max = 1000) String imageUrl,
        @NotNull @PastOrPresent LocalDate visitedAt,
        @Size(max = 200) String note,
        @Valid TasteNotesRequest tasteNotes,
        @NotNull RevisitIntention revisit,
        @JsonProperty("public") @NotNull Boolean isPublic
) {
    public record TasteNotesRequest(
            List<String> broth,
            List<String> noodle,
            List<String> seasoning,
            List<String> topping
    ) {
        public static TasteNotesRequest empty() {
            return new TasteNotesRequest(List.of(), List.of(), List.of(), List.of());
        }
    }

    public TasteNotesRequest normalizedTasteNotes() {
        return tasteNotes == null ? TasteNotesRequest.empty() : tasteNotes;
    }
}
