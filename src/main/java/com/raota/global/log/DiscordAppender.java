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
        int color = 16711680;

        String stackTrace = "";
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        }

        String summary = truncate(event.getFormattedMessage(), 300);
        String stackTracePreview = stackTrace.isEmpty() ? "-" : "```java\n" + truncate(stackTrace, 1200) + "\n```";

        DiscordEmbed embed = DiscordEmbed.builder()
                .title("🚨 [ERROR] " + truncate(event.getFormattedMessage(), 180))
                .description(summary)
                .color(color)
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                .fields(List.of(
                        EmbedField.builder().name("Logger").value("`" + event.getLoggerName() + "`").inline(true).build(),
                        EmbedField.builder().name("Thread").value("`" + event.getThreadName() + "`").inline(true).build(),
                        EmbedField.builder().name("Stack Trace").value(stackTracePreview).inline(false).build()
                ))
                .build();

        return DiscordMessage.builder()
                .embeds(List.of(embed))
                .build();
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
