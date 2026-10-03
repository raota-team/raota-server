package com.raota.mobile.ramenlog.domain.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MobileTasteNote {

    BROTH_01("BROTH", "진해요"), BROTH_02("BROTH", "깔끔해요"), BROTH_03("BROTH", "감칠맛 좋아요"), BROTH_04("BROTH", "기름져요"),
    BROTH_05("BROTH", "어패류 향"), NOODLE_01("NOODLE", "탄력 있어요"), NOODLE_02("NOODLE", "단단해요"),
    NOODLE_03("NOODLE", "부드러워요"), NOODLE_04("NOODLE", "국물이 잘 배어요"), NOODLE_05("NOODLE", "양 많아요"),
    SEASONING_01("SEASONING", "딱 좋아요"), SEASONING_02("SEASONING", "슴슴해요"), SEASONING_03("SEASONING", "짭짤해요"),
    SEASONING_04("SEASONING", "매콤해요"), SEASONING_05("SEASONING", "밥 생각나요"), TOPPING_01("TOPPING", "차슈 좋아요"),
    TOPPING_02("TOPPING", "계란 좋아요"), TOPPING_03("TOPPING", "멘마 좋아요"), TOPPING_04("TOPPING", "파 향 좋아요"),
    TOPPING_05("TOPPING", "구성 알차요");

    private static final Set<String> CODES = Arrays.stream(values())
        .map(Enum::name)
        .collect(Collectors.toUnmodifiableSet());

    private final String category;

    private final String label;

    MobileTasteNote(String category, String label) {
        this.category = category;
        this.label = label;
    }

    public String category() {
        return category;
    }

    public String label() {
        return label;
    }

    public static boolean isCode(String code) {
        return CODES.contains(code);
    }

}
