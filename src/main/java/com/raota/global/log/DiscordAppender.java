package com.raota.global.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class DiscordAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    @Setter
    public String webHookUrl = "";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        if (iLoggingEvent.getLevel().isGreaterOrEqual(Level.WARN)) {
            sendDiscordWebhook(iLoggingEvent);
        }
    }

    private void sendDiscordWebhook(ILoggingEvent event) {
        if (webHookUrl == null || webHookUrl.isBlank() || webHookUrl.contains("${")) {
            return;
        }

        try {
            DiscordMessage payload = createMessage(event);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webHookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            addError("Failed to send log to Discord", e);
        }
    }

    private DiscordMessage createMessage(ILoggingEvent event){
        boolean isError = event.getLevel().isGreaterOrEqual(Level.ERROR);
        int color = isError ? 16711680 : 16776960; // ERROR: red, WARN: yellow
        String levelIcon = isError ? "🚨" : "⚠️";
        String levelName = event.getLevel().toString();

        String stackTrace = "";
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        }

        String summary = truncate(event.getFormattedMessage(), 300);

        List<EmbedField> fields = new ArrayList<>();
        fields.add(EmbedField.builder().name("Logger").value("`" + event.getLoggerName() + "`").inline(true).build());
        fields.add(EmbedField.builder().name("Thread").value("`" + event.getThreadName() + "`").inline(true).build());
        if (!stackTrace.isEmpty()) {
            fields.add(EmbedField.builder()
                    .name("Stack Trace")
                    .value("```java\n" + truncate(firstLines(stackTrace, 4), 1200) + "\n```")
                    .inline(false)
                    .build());
        }

        DiscordEmbed embed = DiscordEmbed.builder()
                .title(levelIcon + " [" + levelName + "] " + truncate(event.getFormattedMessage(), 180))
                .description(summary)
                .color(color)
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                .fields(fields)
                .build();

        return DiscordMessage.builder()
                .embeds(List.of(embed))
                .build();
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String firstLines(String str, int maxLines) {
        String[] lines = str.split("\\R");
        int lineCount = Math.min(lines.length, maxLines);
        return String.join("\n", java.util.Arrays.copyOf(lines, lineCount));
    }
}
