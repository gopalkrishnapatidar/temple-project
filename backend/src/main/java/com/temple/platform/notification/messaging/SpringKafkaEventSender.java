package com.temple.platform.notification.messaging;

import com.temple.platform.notification.config.NotificationKafkaProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(prefix = "app.notification.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringKafkaEventSender implements KafkaEventSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration ackTimeout;

    public SpringKafkaEventSender(
            KafkaTemplate<String, String> kafkaTemplate,
            NotificationKafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.ackTimeout = properties.getProducerAckTimeout();
    }

    @Override
    public void send(String topic, String key, String payloadJson) {
        try {
            kafkaTemplate.send(new ProducerRecord<>(topic, key, payloadJson))
                    .get(ackTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaUnavailableException(ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaUnavailableException(ex);
        }
    }
}
