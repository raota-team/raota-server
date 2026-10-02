package com.raota.mobile.ramenlog.application.command;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.LocalDate;
import java.util.List;

public record MobileCreateRamenLogCommand(String shopId, LocalDate visitedAt, String menuName, String ramenType,
        Scores scores, RevisitIntention revisitIntention, String note, List<String> tasteNoteCodes,
        LogVisibility visibility, List<String> imageUrls) {

    public record Scores(int satisfaction, int brothDensity, int noodleFirmness, int topping) {
    }

}
