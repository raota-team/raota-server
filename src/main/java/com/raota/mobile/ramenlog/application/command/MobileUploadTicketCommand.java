package com.raota.mobile.ramenlog.application.command;

import java.util.List;

public record MobileUploadTicketCommand(Purpose purpose, List<File> files) {

    public enum Purpose {

        RAMEN_LOG, PROFILE

    }

    public record File(String contentType, String extension) {
    }

}
