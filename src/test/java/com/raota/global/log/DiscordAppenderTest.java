package com.raota.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Test;

class DiscordAppenderTest {

    /**
     * 웹훅 주소가 기본값 none이면 전송을 끈 것으로 본다. 오류 상태가 남으면 같은 JVM에서 다음 Spring 컨텍스트의 Logback 초기화가 이를
     * 설정 오류로 보고 기동에 실패한다(운영 프로필 테스트 뒤 다른 통합 테스트가 간헐적으로 실패하던 원인).
     */
    @Test
    void 웹훅_주소가_none이면_보내지_않고_오류_상태도_남기지_않는다() {
        LoggerContext context = new LoggerContext();
        DiscordAppender appender = new DiscordAppender();
        appender.setContext(context);
        appender.setWebHookUrl("none");
        appender.start();

        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("test");
        event.setMessage("boom");
        event.setTimeStamp(System.currentTimeMillis());
        appender.doAppend(event);

        assertThat(context.getStatusManager().getCopyOfStatusList()).noneMatch(s -> s.getLevel() == Status.ERROR);
    }
}
