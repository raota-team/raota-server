package com.raota.mobile.ramenlog.application.result;

import java.util.List;

public record MobileTasteNoteDefinitions(String version, List<Group> groups) {

    public record Group(String category, List<Note> notes) {
    }

    public record Note(String code, String label) {
    }

}
