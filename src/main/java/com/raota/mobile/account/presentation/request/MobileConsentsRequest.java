package com.raota.mobile.account.presentation.request;

import com.raota.mobile.account.application.command.MobileOnboardingCommand;
import com.raota.mobile.account.presentation.request.MobileOnboardingRequest.ConsentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 법적 문서별 동의 결정. 문서 버전은 시행일(YYYY-MM-DD)이다. */
public record MobileConsentsRequest(@NotEmpty List<@NotNull @Valid ConsentRequest> consents) {

    public List<MobileOnboardingCommand.ConsentDecision> toDecisions() {
        return consents.stream()
                .map(consent -> new MobileOnboardingCommand.ConsentDecision(
                        consent.type(), consent.documentVersion(), consent.granted()))
                .toList();
    }
}
