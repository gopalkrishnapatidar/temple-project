package com.temple.platform.notification.repository;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.notification.domain.AggregateType;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.NotificationChannel;
import com.temple.platform.notification.domain.NotificationStatus;
import com.temple.platform.platform.repository.ApplicationMetadataRepository;
import com.temple.platform.support.IsolatedPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IsolatedPostgresIntegrationTest
class NotificationSchemaRepositoryTest {

    @Autowired
    private ApplicationMetadataRepository applicationMetadataRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaVersionIsTenAfterFlyway() {
        assertThat(applicationMetadataRepository.findValue("schema_version")).contains("10");
        assertThat(applicationMetadataRepository.findLatestFlywayVersion()).contains("10");
    }

    @Test
    @Transactional
    void outboxEventIdIsUnique() {
        UUID eventId = UUID.randomUUID();
        insertOutbox(eventId, "booking-1", DomainEventType.BOOKING_CONFIRMED);

        assertThatThrownBy(() -> insertOutbox(eventId, "booking-2", DomainEventType.BOOKING_CANCELLED))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void notificationSourceEventIdIsUnique() {
        long accountId = createAccount();
        UUID sourceEventId = UUID.randomUUID();
        notificationRepository.insertIfAbsent(
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK,
                DomainEventType.BOOKING_CONFIRMED,
                "Title",
                "Message",
                sourceEventId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO notification (
                    notification_reference, account_id, channel, type, status,
                    title, message, source_event_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK.name(),
                DomainEventType.BOOKING_CONFIRMED.name(),
                NotificationStatus.PENDING.name(),
                "Other",
                "Other",
                sourceEventId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void notificationInsertIfAbsentDeduplicatesBySourceEventId() {
        long accountId = createAccount();
        UUID sourceEventId = UUID.randomUUID();

        assertThat(notificationRepository.insertIfAbsent(
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK,
                DomainEventType.PAYMENT_SUCCEEDED,
                "Payment successful",
                "Your payment succeeded.",
                sourceEventId
        )).isPresent();

        assertThat(notificationRepository.insertIfAbsent(
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK,
                DomainEventType.PAYMENT_SUCCEEDED,
                "Payment successful",
                "Duplicate attempt.",
                sourceEventId
        )).isEmpty();

        assertThat(notificationRepository.countByAccountId(accountId)).isEqualTo(1);
    }

    private void insertOutbox(UUID eventId, String aggregateReference, DomainEventType eventType) {
        outboxEventRepository.insert(
                eventId,
                AggregateType.BOOKING,
                aggregateReference,
                eventType,
                1,
                """
                {"eventId":"%s","eventType":"%s","eventVersion":1,"occurredAt":"2026-01-01T00:00:00Z","aggregateReference":"%s","payload":{"accountId":1}}
                """.formatted(eventId, eventType.name(), aggregateReference)
        );
    }

    private long createAccount() {
        return accountRepository.insert(
                "notification-schema-" + UUID.randomUUID() + "@example.com",
                "hash",
                AccountRole.DEVOTEE,
                AccountStatus.ACTIVE
        ).id();
    }
}
