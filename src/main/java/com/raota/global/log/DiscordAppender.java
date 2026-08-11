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

    private static final int EMBED_TITLE_LIMIT = 256;
    private static final int EMBED_DESCRIPTION_LIMIT = 4096;
    private static final int EMBED_FIELD_VALUE_LIMIT = 1024;
    private static final int DISCORD_CODE_BLOCK_OVERHEAD = 11;
    private static final int STACK_EMBED_LIMIT = EMBED_DESCRIPTION_LIMIT - DISCORD_CODE_BLOCK_OVERHEAD;
    private static final int STACK_FIELD_LIMIT = EMBED_FIELD_VALUE_LIMIT - DISCORD_CODE_BLOCK_OVERHEAD;
    private static final int MAX_STACK_EMBEDS = 3;

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
        if (throwableProxy != null) {
            fields.add(EmbedField.builder()
                    .name("Exception")
                    .value("`" + truncate(throwableProxy.getClassName(), STACK_FIELD_LIMIT - 2) + "`")
                    .inline(false)
                    .build());
        }

        List<DiscordEmbed> embeds = new ArrayList<>();
        embeds.add(DiscordEmbed.builder()
                .title(truncate(levelIcon + " [" + levelName + "] " + event.getFormattedMessage(), EMBED_TITLE_LIMIT))
                .description(summary)
                .color(color)
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                .fields(fields)
                .build());

        List<String> stackChunks = splitByLength(stackTrace, STACK_EMBED_LIMIT);
        int chunkCount = Math.min(stackChunks.size(), MAX_STACK_EMBEDS);
        for (int i = 0; i < chunkCount; i++) {
            embeds.add(DiscordEmbed.builder()
                    .title("Stack Trace" + (stackChunks.size() > 1 ? " (" + (i + 1) + "/" + stackChunks.size() + ")" : ""))
                    .description(toCodeBlock(stackChunks.get(i)))
                    .color(color)
                    .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                    .build());
        }

        if (stackChunks.size() > MAX_STACK_EMBEDS) {
            embeds.add(DiscordEmbed.builder()
                    .title("Stack Trace (truncated)")
                    .description("Remaining stack trace omitted due to Discord payload limits.")
                    .color(color)
                    .timestamp(Instant.ofEpochMilli(event.getTimeStamp()).toString())
                    .build());
        }

        return DiscordMessage.builder()
                .embeds(embeds)
                .build();
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String toCodeBlock(String str) {
        return "```java\n" + str + "\n```";
    }

    private List<String> splitByLength(String str, int maxLength) {
        List<String> chunks = new ArrayList<>();
        if (str == null || str.isBlank()) {
            return chunks;
        }

        String[] lines = str.split("\\R");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String safeLine = truncate(line, maxLength);
            String candidate = current.isEmpty() ? safeLine : current + "\n" + safeLine;

            if (candidate.length() > maxLength) {
                chunks.add(current.toString());
                current = new StringBuilder(safeLine);
                continue;
            }

            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(safeLine);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }

        return chunks;
    }
}
