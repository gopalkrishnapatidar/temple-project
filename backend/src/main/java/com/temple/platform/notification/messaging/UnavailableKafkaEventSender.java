package com.temple.platform.notification.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notification.kafka", name = "enabled", havingValue = "false")
public class UnavailableKafkaEventSender implements KafkaEventSender {

    @Override
    public void send(String topic, String key, String payloadJson) {
        throw new KafkaUnavailableException(new IllegalStateException("Kafka integration is disabled"));
    }
}
