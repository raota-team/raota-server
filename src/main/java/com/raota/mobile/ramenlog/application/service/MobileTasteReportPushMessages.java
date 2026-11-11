package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.notification.application.facade.MobilePushMessage;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/** 제목과 본문은 앱 번역과 같다. 앱은 {@code data.url}로 화면을 연다. */
final class MobileTasteReportPushMessages {

    private static final Map<String, String> TASTE_LINK = Map.of("url", "raota://taste");

    private MobileTasteReportPushMessages() {}

    static MobilePushMessage monthly(Locale locale, YearMonth month) {
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, locale);
        return switch (locale.getLanguage()) {
            case "en" ->
                message("Your %s report is ready".formatted(monthName), "Ready to stack up this month's bowls?");
            case "ja" -> message("%sのレポートが届きました".formatted(monthName), "今月の一杯を積み上げてみませんか？");
            case "zh" -> message("%s报告已生成".formatted(monthName), "来把这个月的碗叠起来吧？");
            case "es" ->
                message("Tu informe de %s está listo".formatted(monthName), "¿Apilamos los platos de este mes?");
            default -> message("%s 리포트가 나왔어요".formatted(monthName), "이번 달 그릇을 쌓아 볼까요?");
        };
    }

    private static MobilePushMessage message(String title, String body) {
        return new MobilePushMessage(title, body, TASTE_LINK);
    }
}
