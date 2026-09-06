package com.temple.platform.notification.messaging;

public interface KafkaEventSender {

    void send(String topic, String key, String payloadJson);
}
