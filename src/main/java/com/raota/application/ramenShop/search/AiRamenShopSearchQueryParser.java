package com.raota.application.ramenShop.search;

import com.raota.application.ramenShop.query.ParsedAiRamenShopSearchQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiRamenShopSearchQueryParser {

    private static final Map<String, List<String>> FOOD_ALIASES = Map.of(
            "쇼유", List.of("쇼유", "쇼유라멘", "간장라멘", "간장 베이스"),
            "시오", List.of("시오", "시오라멘", "소금라멘", "소금 베이스"),
            "돈코츠", List.of("돈코츠", "돼지뼈", "진한 국물", "농후한 국물"),
            "츠케멘", List.of("츠케멘", "찍어먹는 라멘", "농후한 소스"),
            "미소", List.of("미소", "미소라멘", "된장라멘", "된장 베이스"),
            "탄탄", List.of("탄탄", "탄탄멘", "매운 라멘", "매콤한 국물"),
            "매운", List.of("매운", "매운 라멘", "매콤한", "카라이", "탄탄멘")
    );

    private static final Map<String, List<String>> REGION_ALIASES = Map.ofEntries(
            Map.entry("홍대", List.of("서울 마포구")),
            Map.entry("합정", List.of("서울 마포구")),
            Map.entry("상수", List.of("서울 마포구")),
            Map.entry("연남", List.of("서울 마포구")),
            Map.entry("망원", List.of("서울 마포구")),
            Map.entry("강남", List.of("서울 강남구", "서울 서초구")),
            Map.entry("성수", List.of("서울 성동구")),
            Map.entry("건대", List.of("서울 광진구")),
            Map.entry("잠실", List.of("서울 송파구")),
            Map.entry("이태원", List.of("서울 용산구")),
            Map.entry("명동", List.of("서울 중구")),
            Map.entry("혜화", List.of("서울 종로구"))
    );

    public ParsedAiRamenShopSearchQuery parse(String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        Set<String> foodKeywords = new HashSet<>();
        Set<String> regions = new HashSet<>();

        FOOD_ALIASES.forEach((key, aliases) -> {
            if (containsAny(normalizedQuery, aliases)) {
                foodKeywords.addAll(aliases);
            }
        });
        REGION_ALIASES.forEach((key, regionValues) -> {
            if (normalizedQuery.contains(key)) {
                regions.addAll(regionValues);
            }
        });

        List<String> expansions = foodKeywords.stream()
                .filter(keyword -> !normalizedQuery.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
        String expandedQuery = expansions.isEmpty()
                ? query
                : query + " " + String.join(" ", expansions);

        return new ParsedAiRamenShopSearchQuery(
                normalizedQuery,
                expandedQuery,
                List.copyOf(foodKeywords),
                List.copyOf(regions)
        );
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }
}
