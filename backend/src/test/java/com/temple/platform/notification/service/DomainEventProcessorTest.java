package com.temple.platform.notification.service;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.notification.delivery.NotificationDelivery;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.Notification;
import com.temple.platform.notification.domain.NotificationStatus;
import com.temple.platform.notification.exception.UnrecoverableDomainEventException;
import com.temple.platform.notification.repository.NotificationRepository;
import com.temple.platform.support.IsolatedPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IsolatedPostgresIntegrationTest
class DomainEventProcessorTest {

    @Autowired
    private DomainEventProcessor domainEventProcessor;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ControllableNotificationDelivery notificationDelivery;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long accountId;

    @BeforeEach
    void resetDelivery() {
        accountId = accountRepository.insert(
                "domain-event-" + UUID.randomUUID() + "@example.com",
                "hash",
                AccountRole.DEVOTEE,
                AccountStatus.ACTIVE
        ).id();
        notificationDelivery.reset();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void validEventCreatesOneNotificationAndMarksSent() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();

        domainEventProcessor.process(bookingConfirmedJson(eventId, accountId, bookingReference));

        assertThat(notificationDelivery.deliveries()).isEqualTo(1);
        assertThat(notificationRepository.countByAccountId(accountId)).isEqualTo(1);
        Notification notification = notificationRepository.findBySourceEventId(eventId).orElseThrow();
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.sentAt()).isNotNull();
        assertThat(notification.type()).isEqualTo(DomainEventType.BOOKING_CONFIRMED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replayOfSameEventIdDoesNotCreateDuplicateNotification() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();
        String json = bookingConfirmedJson(eventId, accountId, bookingReference);

        domainEventProcessor.process(json);
        domainEventProcessor.process(json);

        assertThat(notificationRepository.countByAccountId(accountId)).isEqualTo(1);
        assertThat(notificationDelivery.deliveries()).isEqualTo(1);
        assertThat(notificationRepository.findBySourceEventId(eventId).orElseThrow().status())
                .isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void malformedEventFailsWithUnrecoverableDomainEventException() {
        assertThatThrownBy(() -> domainEventProcessor.process("{not-json"))
                .isInstanceOf(UnrecoverableDomainEventException.class)
                .hasMessageContaining("Malformed domain event");
        assertThat(notificationRepository.countByAccountId(accountId)).isZero();
    }

    @Test
    void unsupportedEventFailsWithUnrecoverableDomainEventException() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {
                  "eventId":"%s",
                  "eventType":"PAYMENT_SUCCEEDED",
                  "eventVersion":0,
                  "occurredAt":"2026-01-01T00:00:00Z",
                  "aggregateReference":"ref",
                  "payload":{"accountId":%d,"paymentReference":"pay-1"}
                }
                """.formatted(eventId, accountId);

        assertThatThrownBy(() -> domainEventProcessor.process(json))
                .isInstanceOf(UnrecoverableDomainEventException.class)
                .hasMessageContaining("Invalid event version");
        assertThat(notificationRepository.countByAccountId(accountId)).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deliveryFailurePersistsExactlyOneFailedNotification() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();
        notificationDelivery.failOnDeliver();

        assertThatThrownBy(() -> domainEventProcessor.process(
                bookingConfirmedJson(eventId, accountId, bookingReference)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("delivery failed");

        assertThat(countNotificationsBySourceEventId(eventId)).isEqualTo(1);
        Notification notification = notificationRepository.findBySourceEventId(eventId).orElseThrow();
        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.sentAt()).isNull();
        assertThat(notificationDelivery.deliveries()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deliveryFailurePersistsSanitizedFailureReason() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();
        notificationDelivery.failOnDeliverWithLongMessage();

        assertThatThrownBy(() -> domainEventProcessor.process(
                bookingConfirmedJson(eventId, accountId, bookingReference)))
                .isInstanceOf(RuntimeException.class);

        Notification notification = notificationRepository.findBySourceEventId(eventId).orElseThrow();
        assertThat(notification.failureReason()).startsWith("delivery failed");
        assertThat(notification.failureReason()).hasSize(512);
        assertThat(notification.failureReason()).doesNotContain("\r").doesNotContain("\n").doesNotContain("\t");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void retryAfterFailureReusesSameRowAndTransitionsToSent() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();
        String json = bookingConfirmedJson(eventId, accountId, bookingReference);
        notificationDelivery.failOnDeliver();

        assertThatThrownBy(() -> domainEventProcessor.process(json))
                .isInstanceOf(RuntimeException.class);

        Notification failed = notificationRepository.findBySourceEventId(eventId).orElseThrow();
        assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);

        notificationDelivery.succeedOnDeliver();
        domainEventProcessor.process(json);

        Notification sent = notificationRepository.findBySourceEventId(eventId).orElseThrow();
        assertThat(sent.id()).isEqualTo(failed.id());
        assertThat(sent.notificationReference()).isEqualTo(failed.notificationReference());
        assertThat(sent.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.sentAt()).isNotNull();
        assertThat(sent.failureReason()).isNull();
        assertThat(notificationDelivery.deliveries()).isEqualTo(2);
        assertThat(countNotificationsBySourceEventId(eventId)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replayAfterSentDoesNotDeliverAgain() {
        UUID eventId = UUID.randomUUID();
        UUID bookingReference = UUID.randomUUID();
        String json = bookingConfirmedJson(eventId, accountId, bookingReference);

        domainEventProcessor.process(json);
        domainEventProcessor.process(json);
        domainEventProcessor.process(json);

        assertThat(notificationDelivery.deliveries()).isEqualTo(1);
        assertThat(countNotificationsBySourceEventId(eventId)).isEqualTo(1);
        assertThat(notificationRepository.findBySourceEventId(eventId).orElseThrow().status())
                .isEqualTo(NotificationStatus.SENT);
    }

    private long countNotificationsBySourceEventId(UUID sourceEventId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE source_event_id = ?",
                Long.class,
                sourceEventId
        );
        return count == null ? 0L : count;
    }

    private static String bookingConfirmedJson(UUID eventId, long accountId, UUID bookingReference) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"BOOKING_CONFIRMED",
                  "eventVersion":1,
                  "occurredAt":"2026-01-01T00:00:00Z",
                  "aggregateReference":"%s",
                  "payload":{
                    "accountId":%d,
                    "bookingReference":"%s"
                  }
                }
                """.formatted(eventId, bookingReference, accountId, bookingReference);
    }

    @TestConfiguration
    static class DeliveryTestConfig {

        @Bean
        @Primary
        ControllableNotificationDelivery controllableNotificationDelivery() {
            return new ControllableNotificationDelivery();
        }
    }

    static class ControllableNotificationDelivery implements NotificationDelivery {

        private final AtomicInteger deliveries = new AtomicInteger();
        private volatile boolean fail;
        private volatile boolean failWithLongMessage;

        void reset() {
            deliveries.set(0);
            fail = false;
            failWithLongMessage = false;
        }

        void failOnDeliver() {
            fail = true;
            failWithLongMessage = false;
        }

        void failOnDeliverWithLongMessage() {
            fail = true;
            failWithLongMessage = true;
        }

        void succeedOnDeliver() {
            fail = false;
            failWithLongMessage = false;
        }

        int deliveries() {
            return deliveries.get();
        }

        @Override
        public void deliver(Notification notification) {
            deliveries.incrementAndGet();
            if (!fail) {
                return;
            }
            if (failWithLongMessage) {
                throw new RuntimeException("delivery failed\r\n\t" + "x".repeat(700));
            }
            throw new RuntimeException("delivery failed");
        }
    }
}
