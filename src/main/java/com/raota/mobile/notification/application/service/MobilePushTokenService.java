package com.raota.mobile.notification.application.service;

import com.raota.mobile.notification.domain.model.PushLanguage;
import com.raota.mobile.notification.domain.model.PushPlatform;
import com.raota.mobile.notification.domain.repository.MobilePushTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토큰이 있는 기기가 알림을 받는 기기다. 앱은 알림을 끄거나 로그아웃할 때 토큰을 해제한다. */
@Service
@RequiredArgsConstructor
public class MobilePushTokenService {

    private final MobilePushTokenRepository tokens;

    private final Clock clock;

    @Transactional
    public void register(Long userId, String token, PushPlatform platform, PushLanguage language) {
        tokens.upsert(
                userId,
                token,
                platform.name(),
                language.name(),
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
    }

    /** 내 토큰만 지운다. 없거나 다른 회원의 토큰이어도 성공으로 본다. */
    @Transactional
    public void unregister(Long userId, String token) {
        tokens.deleteOwned(userId, token);
    }

    @Transactional
    public void removeUnregistered(Set<String> unregistered) {
        if (!unregistered.isEmpty()) {
            tokens.deleteByTokenIn(unregistered);
        }
    }

    @Transactional
    public void removeForPurgedUser(Long userId) {
        tokens.deleteForUser(userId);
    }
}
