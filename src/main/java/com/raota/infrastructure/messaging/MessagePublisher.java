package com.raota.infrastructure.messaging;

public interface MessagePublisher {
    /**
     * @param topic 메시지를 발행할 주제 (Queue/Stream 이름)
     * @param payload 전송할 데이터 객체
     */
    void publish(String topic, Object payload);
}