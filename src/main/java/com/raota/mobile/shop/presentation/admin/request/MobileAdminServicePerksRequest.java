package com.raota.mobile.shop.presentation.admin.request;

import com.raota.mobile.shop.application.command.MobileAdminServicePerkCommand;
import com.raota.mobile.shop.domain.model.ServicePerkStatus;
import com.raota.mobile.shop.domain.model.ServicePerkType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MobileAdminServicePerksRequest(@NotNull @Size(max = 4) List<@NotNull @Valid Perk> perks) {

    public List<MobileAdminServicePerkCommand> toCommands() {
        return perks.stream()
            .map(perk -> new MobileAdminServicePerkCommand(perk.type(), perk.status(), perk.price(),
                    text(perk.condition())))
            .toList();
    }

    private static String text(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    public record Perk(@NotNull ServicePerkType type, @NotNull ServicePerkStatus status, @PositiveOrZero Integer price,
            @Size(max = 1000) String condition) {
    }

}
