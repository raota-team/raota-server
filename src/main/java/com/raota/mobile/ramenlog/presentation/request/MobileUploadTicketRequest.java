package com.raota.mobile.ramenlog.presentation.request;

import com.raota.mobile.ramenlog.application.command.MobileUploadTicketCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MobileUploadTicketRequest(@NotNull MobileUploadTicketCommand.Purpose purpose,
        @NotNull @Size(min = 1, max = 3) List<@NotNull @Valid File> files) {

    public record File(@NotBlank String contentType, @NotBlank String extension) {
    }

    public MobileUploadTicketCommand toCommand() {
        return new MobileUploadTicketCommand(purpose,
                files.stream()
                    .map(file -> new MobileUploadTicketCommand.File(file.contentType(), file.extension()))
                    .toList());
    }

}
