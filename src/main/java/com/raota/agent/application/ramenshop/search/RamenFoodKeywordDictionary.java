package com.raota.agent.application.ramenshop.search;

import java.util.List;
import java.util.Locale;

final class RamenFoodKeywordDictionary {

    static final List<FoodKeywordGroup> GROUPS = List.of(
            new FoodKeywordGroup("쇼유", List.of("쇼유", "쇼유라멘", "간장", "간장라멘", "간장 베이스")),
            new FoodKeywordGroup("시오", List.of("시오", "시오라멘", "소금", "소금라멘", "소금 베이스")),
            new FoodKeywordGroup("돈코츠", List.of("돈코츠", "돼지뼈", "진한 국물", "농후한 국물")),
            new FoodKeywordGroup("츠케멘", List.of("츠케멘", "찍어먹는 라멘", "농후한 소스")),
            new FoodKeywordGroup("미소", List.of("미소", "미소라멘", "된장", "된장라멘", "된장 베이스")),
            new FoodKeywordGroup("탄탄", List.of("탄탄", "탄탄멘", "매운 라멘", "매콤한 국물")),
            new FoodKeywordGroup("매운", List.of("매운", "매운 라멘", "매콤한", "카라이", "탄탄멘"))
    );

    private RamenFoodKeywordDictionary() {
    }

    static List<String> detectTypes(String normalizedQuery) {
        return GROUPS.stream()
                .filter(group -> containsAny(normalizedQuery, group.aliases()))
                .map(FoodKeywordGroup::type)
                .distinct()
                .toList();
    }

    static List<String> aliasesFor(List<String> foodTypes) {
        return GROUPS.stream()
                .filter(group -> foodTypes.contains(group.type()))
                .flatMap(group -> group.aliases().stream())
                .distinct()
                .toList();
    }

    static List<String> conflictAliasesFor(List<String> foodTypes) {
        return GROUPS.stream()
                .filter(group -> !foodTypes.contains(group.type()))
                .filter(group -> !"매운".equals(group.type()))
                .flatMap(group -> group.aliases().stream())
                .distinct()
                .toList();
    }

    private static boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    record FoodKeywordGroup(String type, List<String> aliases) {
    }
}
