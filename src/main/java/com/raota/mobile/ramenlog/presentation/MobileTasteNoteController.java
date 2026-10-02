package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileTasteNoteDefinitions;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/taste-note-definitions")
@RequiredArgsConstructor
public class MobileTasteNoteController {

    private final MobileRamenLogQueryService logs;

    @GetMapping
    public MobileApiResponse<MobileTasteNoteDefinitions> definitions() {
        return MobileApiResponse.success(logs.definitions());
    }

}
