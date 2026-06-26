package com.raota.application.ramenShop.search;

import com.raota.application.ramenShop.query.ParsedAiRamenShopSearchQuery;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Component;

@Component
public class AiRamenShopSearchQueryParser {

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
        Set<String> regions = new HashSet<>();

        List<String> foodTypes = RamenFoodKeywordDictionary.detectTypes(normalizedQuery);
        List<String> foodKeywords = RamenFoodKeywordDictionary.aliasesFor(foodTypes);
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
                foodTypes,
                foodKeywords,
                List.copyOf(regions)
        );
    }
}
