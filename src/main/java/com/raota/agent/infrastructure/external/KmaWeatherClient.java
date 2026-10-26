package com.raota.agent.infrastructure.external;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 기상청 단기예보(getVilageFcst)로 오늘(KST) 서울 날씨를 한 문장으로 만든다.
 *
 * <p>
 * 중기전망(getMidFcst)은 3일 뒤부터의 흐름만 다뤄 오늘 기온이 없으므로 쓰지 않는다. 02시 발표(그 전에는 전날 23시 발표)를 읽어야 오늘
 * 최저·최고기온(TMN·TMX)이 함께 온다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmaWeatherClient {

    static final String UNAVAILABLE = "기상 정보를 가져오지 못했습니다. 계절에 맞는 일반적인 라멘을 추천해주세요.";

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 서울 종로구 격자다. */
    private static final int NX = 60;

    private static final int NY = 127;

    private static final String PLACE = "서울";

    /** 발표 뒤 자료가 올라오기까지 10분쯤 걸린다. */
    private static final LocalTime TODAY_BASE_READY = LocalTime.of(2, 15);

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);

    private final KmaProperties kmaProperties;

    private final Clock clock;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getWeatherOutlook() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL));
        LocalDate today = now.toLocalDate();
        LocalDate baseDate = today;
        String baseTime = "0200";
        if (now.toLocalTime().isBefore(TODAY_BASE_READY)) {
            baseDate = today.minusDays(1);
            baseTime = "2300";
        }
        URI uri = UriComponentsBuilder.fromUriString(kmaProperties.baseUrl() + "/getVilageFcst")
            .queryParam("authKey", kmaProperties.resolvedAuthKey())
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 1000)
            .queryParam("dataType", "JSON")
            .queryParam("base_date", baseDate.format(DateTimeFormatter.BASIC_ISO_DATE))
            .queryParam("base_time", baseTime)
            .queryParam("nx", NX)
            .queryParam("ny", NY)
            .build(true)
            .toUri();

        try {
            String outlook = summarize(today, items(restTemplate.getForObject(uri, KmaResponse.class)));
            if (outlook != null) {
                log.info("KMA 단기예보 조회 성공: base={} {}, summary={}", baseDate, baseTime, outlook);
                return outlook;
            }
            log.warn("KMA 단기예보에 오늘 예보가 없음: base={} {}", baseDate, baseTime);
        }
        catch (Exception e) {
            log.error("Failed to fetch weather forecast from KMA", e);
        }
        return UNAVAILABLE;
    }

    /** 오늘 예보 항목만 골라 문장으로 만든다. 오늘 항목이 없으면 {@code null}이다. */
    static String summarize(LocalDate today, List<KmaResponse.Item> items) {
        String date = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<KmaResponse.Item> todays = items.stream().filter(item -> date.equals(item.getFcstDate())).toList();
        if (todays.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        first(todays, "TMN", null).ifPresent(value -> parts.add("최저기온 " + number(value) + "도"));
        first(todays, "TMX", null).ifPresent(value -> parts.add("최고기온 " + number(value) + "도"));
        first(todays, "SKY", "0900").map(KmaWeatherClient::sky).ifPresent(sky -> parts.add("오전 하늘 " + sky));
        first(todays, "SKY", "1500").map(KmaWeatherClient::sky).ifPresent(sky -> parts.add("오후 하늘 " + sky));
        parts.add(precipitation(todays));
        values(todays, "POP").stream()
            .max(BigDecimal::compareTo)
            .ifPresent(pop -> parts.add("강수확률 최대 " + pop.toPlainString() + "%"));
        List<BigDecimal> humidity = values(todays, "REH");
        if (!humidity.isEmpty()) {
            BigDecimal sum = humidity.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            parts.add("평균 습도 " + sum.divide(BigDecimal.valueOf(humidity.size()), 0, RoundingMode.HALF_UP) + "%");
        }
        values(todays, "WSD").stream()
            .max(BigDecimal::compareTo)
            .ifPresent(wind -> parts.add("최대 풍속 " + wind.stripTrailingZeros().toPlainString() + "m/s"));
        return today.format(DISPLAY_DATE) + " " + PLACE + " 날씨 예보: " + String.join(", ", parts) + ".";
    }

    private static List<KmaResponse.Item> items(KmaResponse response) {
        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return List.of();
        }
        return response.getResponse().getBody().getItems().getItem();
    }

    /** {@code fcstTime}이 {@code null}이면 시각을 보지 않는다. */
    private static Optional<String> first(List<KmaResponse.Item> items, String category, String fcstTime) {
        return items.stream()
            .filter(item -> category.equals(item.getCategory()))
            .filter(item -> fcstTime == null || fcstTime.equals(item.getFcstTime()))
            .map(KmaResponse.Item::getFcstValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

    private static List<BigDecimal> values(List<KmaResponse.Item> items, String category) {
        List<BigDecimal> values = new ArrayList<>();
        for (KmaResponse.Item item : items) {
            if (category.equals(item.getCategory())) {
                try {
                    values.add(new BigDecimal(item.getFcstValue().trim()));
                }
                catch (RuntimeException ignored) {
                    // 결측값(빈 문자열 등)은 건너뛴다
                }
            }
        }
        return values;
    }

    private static String number(String value) {
        return new BigDecimal(value.trim()).stripTrailingZeros().toPlainString();
    }

    private static String sky(String code) {
        return switch (code.trim()) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> null;
        };
    }

    /** 강수형태(PTY): 0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기. */
    private static String precipitation(List<KmaResponse.Item> items) {
        Set<String> kinds = new LinkedHashSet<>();
        for (KmaResponse.Item item : items) {
            if ("PTY".equals(item.getCategory()) && item.getFcstValue() != null) {
                switch (item.getFcstValue().trim()) {
                    case "1" -> kinds.add("비");
                    case "2" -> kinds.add("비/눈");
                    case "3" -> kinds.add("눈");
                    case "4" -> kinds.add("소나기");
                    default -> {
                    }
                }
            }
        }
        if (kinds.isEmpty()) {
            return "비나 눈 소식 없음";
        }
        return String.join("·", kinds) + " 예보";
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

            private List<Item> item;

        }

        @lombok.Data
        public static class Item {

            private String category;

            private String fcstDate;

            private String fcstTime;

            private String fcstValue;

        }

    }

}
