package com.temple.platform.notification.messaging;

import com.temple.platform.notification.config.NotificationKafkaProperties;
import com.temple.platform.notification.domain.AggregateType;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.OutboxEvent;
import com.temple.platform.notification.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class OutboxPublisherTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ControllableKafkaEventSender kafkaEventSender;

    @Autowired
    private NotificationKafkaProperties kafkaProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSender() {
        jdbcTemplate.update("DELETE FROM outbox_event");
        kafkaEventSender.reset();
    }

    @Test
    @Transactional
    void successfulKafkaSendMarksEventPublished() {
        OutboxEvent event = insertUnpublishedEvent();

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEventRepository.findUnpublished(10)).isEmpty();
        assertThat(kafkaEventSender.lastTopic()).isEqualTo(kafkaProperties.getTopic());
        assertThat(kafkaEventSender.lastKey()).isEqualTo(event.aggregateReference());
        assertThat(kafkaEventSender.lastPayload()).contains(event.eventId().toString());
        assertThat(publishedAt(event.id())).isNotNull();
        assertThat(publishAttempts(event.id())).isZero();
    }

    @Test
    @Transactional
    void kafkaUnavailableLeavesEventUnpublishedAndIncrementsPublishAttempts() {
        OutboxEvent event = insertUnpublishedEvent();
        kafkaEventSender.failWithKafkaUnavailable();

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEventRepository.findUnpublished(10))
                .extracting(OutboxEvent::id)
                .containsExactly(event.id());
        assertThat(publishAttempts(event.id())).isEqualTo(1);
        assertThat(lastError(event.id())).isEqualTo("Kafka is unavailable");
        assertThat(publishedAt(event.id())).isNull();
    }

    @Test
    @Transactional
    void failureStoresOnlySanitizedBoundedErrorInformation() {
        OutboxEvent event = insertUnpublishedEvent();
        kafkaEventSender.failWithLongRuntimeMessage();

        outboxPublisher.publishPendingEvents();

        String error = lastError(event.id());
        assertThat(error).doesNotContain("\r").doesNotContain("\n").doesNotContain("\t");
        assertThat(error).hasSize(512);
        assertThat(publishAttempts(event.id())).isEqualTo(1);
        assertThat(outboxEventRepository.findUnpublished(10)).hasSize(1);
    }

    private OutboxEvent insertUnpublishedEvent() {
        UUID eventId = UUID.randomUUID();
        String aggregateReference = UUID.randomUUID().toString();
        String payload = """
                {"eventId":"%s","eventType":"BOOKING_CONFIRMED","eventVersion":1,"occurredAt":"2026-01-01T00:00:00Z","aggregateReference":"%s","payload":{"accountId":1,"bookingReference":"%s"}}
                """.formatted(eventId, aggregateReference, aggregateReference);
        outboxEventRepository.insert(
                eventId,
                AggregateType.BOOKING,
                aggregateReference,
                DomainEventType.BOOKING_CONFIRMED,
                1,
                payload
        );
        List<OutboxEvent> pending = outboxEventRepository.findUnpublished(1);
        assertThat(pending).hasSize(1);
        return pending.getFirst();
    }

    private Instant publishedAt(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT published_at FROM outbox_event WHERE id = ?",
                (rs, rowNum) -> {
                    var value = rs.getObject("published_at", java.time.OffsetDateTime.class);
                    return value == null ? null : value.toInstant();
                },
                id
        );
    }

    private int publishAttempts(long id) {
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT publish_attempts FROM outbox_event WHERE id = ?",
                Integer.class,
                id
        );
        return attempts == null ? 0 : attempts;
    }

    private String lastError(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM outbox_event WHERE id = ?",
                String.class,
                id
        );
    }

    @TestConfiguration
    static class KafkaSenderTestConfig {

        @Bean
        @Primary
        ControllableKafkaEventSender controllableKafkaEventSender() {
            return new ControllableKafkaEventSender();
        }
    }

    static class ControllableKafkaEventSender implements KafkaEventSender {

        private final AtomicReference<String> lastTopic = new AtomicReference<>();
        private final AtomicReference<String> lastKey = new AtomicReference<>();
        private final AtomicReference<String> lastPayload = new AtomicReference<>();
        private volatile RuntimeException failure;

        void reset() {
            failure = null;
            lastTopic.set(null);
            lastKey.set(null);
            lastPayload.set(null);
        }

        void failWithKafkaUnavailable() {
            failure = new KafkaUnavailableException(new IllegalStateException("broker down"));
        }

        void failWithLongRuntimeMessage() {
            failure = new RuntimeException("bad\r\n\t" + "x".repeat(700));
        }

        String lastTopic() {
            return lastTopic.get();
        }

        String lastKey() {
            return lastKey.get();
        }

        String lastPayload() {
            return lastPayload.get();
        }

        @Override
        public void send(String topic, String key, String payloadJson) {
            if (failure != null) {
                throw failure;
            }
            lastTopic.set(topic);
            lastKey.set(key);
            lastPayload.set(payloadJson);
        }
    }
}
