package com.raota.agent.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.agent.infrastructure.external.KmaWeatherClient.KmaResponse.Item;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class KmaWeatherClientTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Test
    void 오늘_예보만_골라_날짜와_기온_하늘_강수를_쓴다() {
        List<Item> items = List.of(item("20261026", "0600", "TMN", "14.0"), item("20261026", "1500", "TMX", "19.0"),
                item("20261026", "0900", "SKY", "1"), item("20261026", "1500", "SKY", "3"),
                item("20261026", "0900", "PTY", "0"), item("20261026", "1500", "PTY", "1"),
                item("20261026", "0900", "POP", "20"), item("20261026", "1500", "POP", "60"),
                item("20261026", "0900", "REH", "50"), item("20261026", "1500", "REH", "70"),
                item("20261026", "0900", "WSD", "2.5"), item("20261026", "1500", "WSD", "4.1"),
                // 내일 예보는 오늘 문장에 섞이면 안 된다
                item("20261027", "0600", "TMN", "3.0"), item("20261027", "1500", "TMX", "25.0"),
                item("20261027", "1500", "PTY", "3"), item("20261027", "1500", "POP", "90"));

        assertThat(KmaWeatherClient.summarize(TODAY, items))
            .isEqualTo("10월 26일 월요일 서울 날씨 예보: 최저기온 14도, 최고기온 19도, 오전 하늘 맑음, 오후 하늘 구름많음, 비 예보, "
                    + "강수확률 최대 60%, 평균 습도 60%, 최대 풍속 4.1m/s.");
    }

    @Test
    void 오늘_예보가_없으면_문장을_만들지_않는다() {
        assertThat(KmaWeatherClient.summarize(TODAY, List.of(item("20261027", "1500", "TMX", "25.0")))).isNull();
    }

    private static Item item(String date, String time, String category, String value) {
        Item item = new Item();
        item.setFcstDate(date);
        item.setFcstTime(time);
        item.setCategory(category);
        item.setFcstValue(value);
        return item;
    }

}
