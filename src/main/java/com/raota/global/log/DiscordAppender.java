package com.raota.global.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String webHookUrl = "";

    public void setWebHookUrl(String webHookUrl) {
        if (webHookUrl != null) {
            this.webHookUrl = webHookUrl;
        }
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        if (iLoggingEvent.getLevel().isGreaterOrEqual(Level.ERROR)) {
            sendDiscordWebhook(iLoggingEvent);
        }
    }

    private void sendDiscordWebhook(ILoggingEvent event) {
        if (webHookUrl == null || webHookUrl.isBlank() || webHookUrl.equals("none") || webHookUrl.contains("${")) {
            return;
        }

        try {
            // 간단한 JSON 페이로드 수동 생성 (의존성 최소화)
            String title = truncate(event.getFormattedMessage(), 250).replace("\"", "\\\"");
            String stackTrace = "";
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                stackTrace = ThrowableProxyUtil.asString(throwableProxy);
            }
            String description = stackTrace.isEmpty() ? title : "```java\\n" + truncate(stackTrace, 1800).replace("\"", "\\\"").replace("\n", "\\n") + "\\n```";
            int color = event.getLevel().isGreaterOrEqual(Level.ERROR) ? 16711680 : 16776960;

            String jsonPayload = String.format(
                "{\"embeds\": [{\"title\": \"🚨 [%s] %s\", \"description\": \"%s\", \"color\": %d, \"fields\": [" +
                "{\"name\": \"Logger\", \"value\": \"`%s`\", \"inline\": true}," +
                "{\"name\": \"Thread\", \"value\": \"`%s`\", \"inline\": true}" +
                "]}]}",
                event.getLevel(), title, description, color, event.getLoggerName(), event.getThreadName()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webHookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            addError("Failed to send error log to Discord", e);
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        if (maxLength <= 3) return "...";
        return str.substring(0, maxLength - 3) + "...";
    }
}
