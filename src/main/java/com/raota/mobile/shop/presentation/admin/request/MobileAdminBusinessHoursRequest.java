package com.raota.mobile.shop.presentation.admin.request;

import com.raota.mobile.shop.application.command.MobileAdminBusinessHourCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public record MobileAdminBusinessHoursRequest(@NotNull @Size(max = 7) List<@NotNull @Valid Hour> hours) {

    public List<MobileAdminBusinessHourCommand> toCommands() {
        return hours.stream()
            .map(hour -> new MobileAdminBusinessHourCommand(hour.dayOfWeek(), hour.closed(), hour.opensAt(),
                    hour.closesAt(), hour.breakStart(), hour.breakEnd(), hour.lastOrderAt()))
            .toList();
    }

    /** 시간은 HH:mm이다. */
    public record Hour(@NotNull @Min(1) @Max(7) Integer dayOfWeek, boolean closed, LocalTime opensAt,
            LocalTime closesAt, LocalTime breakStart, LocalTime breakEnd, LocalTime lastOrderAt) {
    }

}
