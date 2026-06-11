package com.raota.infrastructure.external;

import com.raota.infrastructure.config.KmaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaWeatherClient {

    private final KmaProperties kmaProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 중기전망조회(getMidFcst) API를 호출하여 기상전망(wfSv) 텍스트를 가져온다.
     */
    public String getWeatherOutlook() {
        URI uri = UriComponentsBuilder.fromUriString(kmaProperties.baseUrl() + "/getMidFcst")
                .queryParam("serviceKey", kmaProperties.serviceKey())
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("dataType", "JSON")
                .queryParam("stnId", kmaProperties.stnId())
                .queryParam("tmFc", getBaseTime())
                .build(true) // serviceKey 인코딩 방지를 위해 true 설정
                .toUri();

        try {
            KmaResponse response = restTemplate.getForObject(uri, KmaResponse.class);

            if (response != null && response.getResponse() != null &&
                response.getResponse().getBody() != null &&
                response.getResponse().getBody().getItems() != null &&
                !response.getResponse().getBody().getItems().getItem().isEmpty()) {
                
                return response.getResponse().getBody().getItems().getItem().get(0).getWfSv();
            }
        } catch (Exception e) {
            log.error("Failed to fetch weather outlook from KMA", e);
        }

        return "기상 정보를 가져오지 못했습니다. 계절에 맞는 일반적인 라멘을 추천해주세요.";
    }

    /**
     * 중기예보 발표 시각(06:00, 18:00)을 계산한다.
     */
    private String getBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (hour < 6) {
            // 새벽 6시 이전이면 어제 오후 6시 데이터 사용
            return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "1800";
        } else if (hour < 18) {
            // 오전 6시 ~ 오후 6시 사이면 오늘 오전 6시 데이터 사용
            return date + "0600";
        } else {
            // 오후 6시 이후면 오늘 오후 6시 데이터 사용
            return date + "1800";
        }
    }

    // 내부 응답 매핑용 DTO 클래스들
    @lombok.Data
    public static class KmaResponse {
        private Response response;

        @lombok.Data
        public static class Response {
            private Body body;
        }

        @lombok.Data
        public static class Body {
            private Items items;
        }

        @lombok.Data
        public static class Items {
            private java.util.List<Item> item;
        }

        @lombok.Data
        public static class Item {
            private String wfSv;
        }
    }
}
