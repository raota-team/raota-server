package com.raota.mobile.account.presentation.request;

import com.raota.mobile.account.application.command.MobileOnboardingCommand;
import com.raota.mobile.account.domain.model.ConsentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 닉네임과 각 법적 문서에 대한 동의 여부를 제출한다. */
public record MobileOnboardingRequest(String nickname, @NotEmpty List<@NotNull @Valid ConsentRequest> consents) {

    public MobileOnboardingCommand toCommand() {
        return new MobileOnboardingCommand(nickname,
                consents.stream()
                    .map(consent -> new MobileOnboardingCommand.ConsentDecision(consent.type(),
                            consent.documentVersion(), consent.granted()))
                    .toList());
    }

    public record ConsentRequest(@NotNull ConsentType type,
            @NotBlank @Size(max = 32) @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String documentVersion,
            @NotNull Boolean granted) {
    }

}
