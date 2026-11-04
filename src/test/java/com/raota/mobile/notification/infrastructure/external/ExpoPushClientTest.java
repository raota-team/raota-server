package com.raota.mobile.notification.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.raota.mobile.notification.application.port.PushSender.PushDelivery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushClientTest {

    private static final String SEND_URL = "https://expo.example/--/api/v2/push/send";

    private MockRestServiceServer server;

    private ExpoPushClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExpoPushClient(builder, new ExpoPushProperties("https://expo.example"));
    }

    @Test
    void 알림을_보내고_등록되지_않은_기기라고_답한_토큰만_돌려준다() {
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[a]"))
                .andExpect(jsonPath("$[0].title").value("11월 리포트가 나왔어요"))
                .andExpect(jsonPath("$[0].data.path").value("/taste"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"status":"ok","id":"ticket-a"},
                          {"status":"error","message":"not registered","details":{"error":"DeviceNotRegistered"}},
                          {"status":"error","message":"too big","details":{"error":"MessageTooBig"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var unregistered = client.send(List.of(
                delivery("ExponentPushToken[a]"), delivery("ExponentPushToken[b]"), delivery("ExponentPushToken[c]")));

        assertThat(unregistered).containsExactly("ExponentPushToken[b]");
        server.verify();
    }

    @Test
    void 백_개씩_나눠_보내고_실패한_묶음은_건너뛴_뒤_다음_묶음을_보낸다() {
        List<PushDelivery> deliveries = new ArrayList<>();
        for (int index = 0; index < ExpoPushClient.CHUNK_SIZE + 1; index++) {
            deliveries.add(delivery("ExponentPushToken[" + index + "]"));
        }
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(ExpoPushClient.CHUNK_SIZE))
                .andRespond(withServerError());
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(1))
                .andRespond(withSuccess("""
                        {"data":[{"status":"error","details":{"error":"DeviceNotRegistered"}}]}
                        """, MediaType.APPLICATION_JSON));

        var unregistered = client.send(deliveries);

        assertThat(unregistered).containsExactly("ExponentPushToken[100]");
        server.verify();
    }

    private static PushDelivery delivery(String token) {
        return new PushDelivery(token, "11월 리포트가 나왔어요", "이번 달 그릇을 쌓아 볼까요?", Map.of("path", "/taste"));
    }
}
