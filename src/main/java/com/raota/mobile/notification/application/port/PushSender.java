package com.raota.mobile.notification.application.port;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 푸시 서비스(Expo)로 알림을 보낸다. 보내지 못한 묶음은 기록만 하고 다음 묶음을 보낸다. */
public interface PushSender {

    /** @return 푸시 서비스가 더는 등록되지 않은 기기라고 답한 토큰 */
    Set<String> send(List<PushDelivery> deliveries);

    record PushDelivery(String token, String title, String body, Map<String, String> data) {}
}
