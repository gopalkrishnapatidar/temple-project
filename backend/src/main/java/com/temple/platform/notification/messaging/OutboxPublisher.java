package com.temple.platform.notification.messaging;

import com.temple.platform.notification.config.NotificationKafkaProperties;
import com.temple.platform.notification.domain.OutboxEvent;
import com.temple.platform.notification.repository.OutboxEventRepository;
import com.temple.platform.notification.support.SanitizedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventSender kafkaEventSender;
    private final NotificationKafkaProperties properties;
    private final Clock clock;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaEventSender kafkaEventSender,
            NotificationKafkaProperties properties,
            Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventSender = kafkaEventSender;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notification.kafka.outbox-poll-interval:5s}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findUnpublished(properties.getOutboxBatchSize());
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            kafkaEventSender.send(properties.getTopic(), event.aggregateReference(), event.payloadJson());
            outboxEventRepository.markPublished(event.id(), clock.instant());
        } catch (KafkaUnavailableException ex) {
            log.debug("Kafka unavailable while publishing outbox event {}", event.eventId());
            outboxEventRepository.recordPublishFailure(event.id(), SanitizedError.from(ex));
        } catch (RuntimeException ex) {
            log.warn("Failed to publish outbox event {}", event.eventId(), ex);
            outboxEventRepository.recordPublishFailure(event.id(), SanitizedError.from(ex));
        }
    }
}
