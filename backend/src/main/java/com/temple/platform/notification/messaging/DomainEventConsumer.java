package com.temple.platform.notification.messaging;

import com.temple.platform.notification.exception.UnrecoverableDomainEventException;
import com.temple.platform.notification.service.DomainEventProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notification.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final DomainEventProcessor domainEventProcessor;

    public DomainEventConsumer(DomainEventProcessor domainEventProcessor) {
        this.domainEventProcessor = domainEventProcessor;
    }

    @KafkaListener(
            topics = "${app.notification.kafka.topic}",
            groupId = "${app.notification.kafka.consumer-group}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            throw new UnrecoverableDomainEventException("Empty Kafka record value");
        }
        try {
            domainEventProcessor.process(payload);
        } catch (UnrecoverableDomainEventException ex) {
            log.warn("Unrecoverable domain event offset={} key={}: {}", record.offset(), record.key(), ex.getMessage());
            throw ex;
        }
    }
}
