package com.raota.mobile.ramenlog.application.command;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogEdit;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogScores;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.LocalDate;
import java.util.List;

public record MobileUpdateRamenLogCommand(LocalDate visitedAt, String menuName, String ramenType,
        MobileRamenLogScores scores, RevisitIntention revisitIntention, String note, List<String> tasteNoteCodes,
        LogVisibility visibility, List<String> imageUrls) {

    public MobileRamenLogEdit toEdit() {
        return new MobileRamenLogEdit(visitedAt, menuName, ramenType, scores, revisitIntention, note, tasteNoteCodes,
                visibility);
    }

}
