package com.raota.mobile.account.application.command;

import com.raota.mobile.account.domain.model.ConsentType;
import java.util.List;

/** 닉네임과 법적 문서별 동의 결정을 가입 완료에 전달한다. */
public record MobileOnboardingCommand(String nickname, List<ConsentDecision> consents) {

    public record ConsentDecision(ConsentType type, String documentVersion, boolean granted) {
    }

}
