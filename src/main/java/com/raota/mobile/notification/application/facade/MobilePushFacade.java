package com.raota.mobile.notification.application.facade;

import com.raota.mobile.notification.application.port.PushSender;
import com.raota.mobile.notification.application.port.PushSender.PushDelivery;
import com.raota.mobile.notification.application.service.MobilePushTokenService;
import com.raota.mobile.notification.domain.model.MobilePushToken;
import com.raota.mobile.notification.domain.model.PushLanguage;
import com.raota.mobile.notification.domain.repository.MobilePushTokenRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MobilePushFacade {

    private static final int BATCH_SIZE = 500;

    private final MobilePushTokenRepository tokens;

    private final MobilePushTokenService tokenService;

    private final PushSender sender;

    /**
     * @param messages 기기 언어(앱이 지원하는 5개 중 하나)의 문구를 만든다. 언어마다 한 번만 부른다.
     * @return 보낸 기기 수(푸시 서비스가 받아들였는지와 관계없다)
     */
    public int send(List<Long> userIds, Function<Locale, MobilePushMessage> messages) {
        Map<PushLanguage, MobilePushMessage> byLanguage = new EnumMap<>(PushLanguage.class);
        int sent = 0;
        for (int start = 0; start < userIds.size(); start += BATCH_SIZE) {
            List<PushDelivery> deliveries = new ArrayList<>();
            for (MobilePushToken token :
                    tokens.findByUserIdIn(userIds.subList(start, Math.min(start + BATCH_SIZE, userIds.size())))) {
                MobilePushMessage message =
                        byLanguage.computeIfAbsent(token.getLanguage(), language -> messages.apply(language.locale()));
                deliveries.add(new PushDelivery(token.getToken(), message.title(), message.body(), message.data()));
            }
            if (deliveries.isEmpty()) {
                continue;
            }
            Set<String> unregistered = sender.send(deliveries);
            tokenService.removeUnregistered(unregistered);
            sent += deliveries.size();
        }
        return sent;
    }
}
