package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileUploadTicketResponse;
import com.raota.mobile.ramenlog.application.service.MobileUploadTicketService;
import com.raota.mobile.ramenlog.presentation.request.MobileUploadTicketRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/files/upload-tickets")
@RequiredArgsConstructor
public class MobileUploadTicketController {

    private final MobileUploadTicketService tickets;

    @PostMapping
    public MobileApiResponse<MobileUploadTicketResponse> issue(@Valid @RequestBody MobileUploadTicketRequest request) {
        return MobileApiResponse.success(tickets.issue(request.toCommand()));
    }

}
