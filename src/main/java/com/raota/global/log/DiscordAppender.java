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
        if (iLoggingEvent.getLevel().isGreaterOrEqual(Level.ERROR)) {
            sendDiscordWebhook(iLoggingEvent);
        }
    }

    private void sendDiscordWebhook(ILoggingEvent event) {
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
            addError("Failed to send error log to Discord", e);
        }
    }

    private DiscordMessage createMessage(ILoggingEvent event){
        int color = event.getLevel().isGreaterOrEqual(Level.ERROR) ? 16711680 : 16776960;

        String stackTrace = "";
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        }

        String description = stackTrace.isEmpty() ? event.getFormattedMessage() : "```java\n" + truncate(stackTrace, 4000) + "\n```";

        DiscordEmbed embed = DiscordEmbed.builder()
                .title("🚨 [" + event.getLevel() + "] " + truncate(event.getFormattedMessage(), 250))
                .description(description)
                .color(color)
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                .fields(List.of(
                        EmbedField.builder().name("Logger").value("`" + event.getLoggerName() + "`").inline(true).build(),
                        EmbedField.builder().name("Thread").value("`" + event.getThreadName() + "`").inline(true).build()
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
