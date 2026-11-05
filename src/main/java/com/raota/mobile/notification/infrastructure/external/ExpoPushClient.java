package com.raota.mobile.notification.infrastructure.external;

import com.raota.mobile.notification.application.port.PushSender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** 영수증(receipt)은 조회하지 않는다. 발송 뒤에 밝혀지는 미등록 기기는 다음 발송의 티켓에서 걸러진다. */
@Slf4j
@Component
public class ExpoPushClient implements PushSender {

    static final int CHUNK_SIZE = 100;

    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    private final RestClient client;

    public ExpoPushClient(RestClient.Builder builder, ExpoPushProperties properties) {
        this.client = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Set<String> send(List<PushDelivery> deliveries) {
        Set<String> unregistered = new HashSet<>();
        for (int start = 0; start < deliveries.size(); start += CHUNK_SIZE) {
            List<PushDelivery> chunk = deliveries.subList(start, Math.min(start + CHUNK_SIZE, deliveries.size()));
            try {
                JsonNode response = client.post()
                        .uri("/--/api/v2/push/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(messages(chunk))
                        .retrieve()
                        .body(JsonNode.class);
                collectUnregistered(chunk, response, unregistered);
            } catch (RestClientException exception) {
                log.warn("Expo 푸시 발송 실패: {}개 묶음을 건너뜀", chunk.size(), exception);
            }
        }
        return unregistered;
    }

    private static List<Map<String, Object>> messages(List<PushDelivery> chunk) {
        List<Map<String, Object>> messages = new ArrayList<>(chunk.size());
        for (PushDelivery delivery : chunk) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("to", delivery.token());
            message.put("title", delivery.title());
            message.put("body", delivery.body());
            message.put("data", delivery.data());
            message.put("sound", "default");
            messages.add(message);
        }
        return messages;
    }

    /** 티켓은 보낸 순서와 같은 순서로 온다. */
    private static void collectUnregistered(List<PushDelivery> chunk, JsonNode response, Set<String> unregistered) {
        if (response == null || !response.path("data").isArray()) {
            log.warn("Expo 푸시 응답에 티켓이 없음: {}", response);
            return;
        }
        JsonNode tickets = response.path("data");
        for (int index = 0; index < Math.min(tickets.size(), chunk.size()); index++) {
            JsonNode ticket = tickets.get(index);
            if (DEVICE_NOT_REGISTERED.equals(
                    ticket.path("details").path("error").asString(""))) {
                unregistered.add(chunk.get(index).token());
            }
        }
    }
}
