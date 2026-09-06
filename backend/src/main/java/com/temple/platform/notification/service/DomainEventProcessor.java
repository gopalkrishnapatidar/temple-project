package com.temple.platform.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.notification.delivery.NotificationDelivery;
import com.temple.platform.notification.domain.DomainEvent;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.Notification;
import com.temple.platform.notification.domain.NotificationChannel;
import com.temple.platform.notification.domain.NotificationStatus;
import com.temple.platform.notification.exception.UnrecoverableDomainEventException;
import com.temple.platform.notification.repository.NotificationRepository;
import com.temple.platform.notification.support.SanitizedError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DomainEventProcessor {

    private static final Set<DomainEventType> SUPPORTED = Set.of(
            DomainEventType.PAYMENT_SUCCEEDED,
            DomainEventType.PAYMENT_FAILED,
            DomainEventType.BOOKING_CONFIRMED,
            DomainEventType.BOOKING_CANCELLED
    );

    private final NotificationRepository notificationRepository;
    private final NotificationDelivery notificationDelivery;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate writeTransaction;

    public DomainEventProcessor(
            NotificationRepository notificationRepository,
            NotificationDelivery notificationDelivery,
            ObjectMapper objectMapper,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.notificationRepository = notificationRepository;
        this.notificationDelivery = notificationDelivery;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void process(String eventJson) {
        DomainEvent event = deserialize(eventJson);
        if (!SUPPORTED.contains(event.eventType())) {
            throw new UnrecoverableDomainEventException("Unsupported event type: " + event.eventType());
        }
        Optional<Notification> toDeliver = writeTransaction.execute(status -> prepareForDelivery(event));
        if (toDeliver.isEmpty()) {
            return;
        }
        Notification notification = toDeliver.get();
        try {
            notificationDelivery.deliver(notification);
            writeTransaction.executeWithoutResult(status ->
                    notificationRepository.markSent(notification.id(), clock.instant()));
        } catch (RuntimeException ex) {
            writeTransaction.executeWithoutResult(status ->
                    notificationRepository.markFailed(notification.id(), SanitizedError.from(ex)));
            throw ex;
        }
    }

    private Optional<Notification> prepareForDelivery(DomainEvent event) {
        long accountId = requireAccountId(event);
        NotificationContent content = contentFor(event);
        Optional<Notification> inserted = notificationRepository.insertIfAbsent(
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK,
                event.eventType(),
                content.title(),
                content.message(),
                event.eventId()
        );
        if (inserted.isPresent()) {
            return inserted;
        }
        Notification existing = notificationRepository.findBySourceEventId(event.eventId())
                .orElseThrow(() -> new IllegalStateException(
                        "Notification missing after idempotent insert conflict for event " + event.eventId()));
        if (existing.status() == NotificationStatus.SENT) {
            return Optional.empty();
        }
        return Optional.of(existing);
    }

    private DomainEvent deserialize(String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);
            UUID eventId = UUID.fromString(requiredText(root, "eventId"));
            DomainEventType eventType = DomainEventType.valueOf(requiredText(root, "eventType"));
            int eventVersion = root.path("eventVersion").asInt(-1);
            if (eventVersion < 1) {
                throw new UnrecoverableDomainEventException("Invalid event version");
            }
            String occurredAt = requiredText(root, "occurredAt");
            String aggregateReference = requiredText(root, "aggregateReference");
            JsonNode payload = root.get("payload");
            if (payload == null || payload.isNull()) {
                throw new UnrecoverableDomainEventException("Missing payload");
            }
            return new DomainEvent(
                    eventId,
                    eventType,
                    eventVersion,
                    java.time.Instant.parse(occurredAt),
                    aggregateReference,
                    payload
            );
        } catch (UnrecoverableDomainEventException ex) {
            throw ex;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new UnrecoverableDomainEventException("Malformed domain event", ex);
        } catch (RuntimeException ex) {
            throw new UnrecoverableDomainEventException("Malformed domain event", ex);
        }
    }

    private static long requireAccountId(DomainEvent event) {
        if (!(event.payload() instanceof JsonNode payload)) {
            throw new UnrecoverableDomainEventException("Invalid payload");
        }
        JsonNode accountId = payload.get("accountId");
        if (accountId == null || !accountId.canConvertToLong()) {
            throw new UnrecoverableDomainEventException("Missing accountId");
        }
        return accountId.longValue();
    }

    private static NotificationContent contentFor(DomainEvent event) {
        JsonNode payload = (JsonNode) event.payload();
        return switch (event.eventType()) {
            case PAYMENT_SUCCEEDED -> new NotificationContent(
                    "Payment successful",
                    "Your payment " + text(payload, "paymentReference") + " was successful."
            );
            case PAYMENT_FAILED -> new NotificationContent(
                    "Payment failed",
                    "Your payment " + text(payload, "paymentReference") + " failed."
            );
            case BOOKING_CONFIRMED -> new NotificationContent(
                    "Booking confirmed",
                    "Your booking " + text(payload, "bookingReference") + " is confirmed."
            );
            case BOOKING_CANCELLED -> new NotificationContent(
                    "Booking cancelled",
                    "Your booking " + text(payload, "bookingReference") + " was cancelled."
            );
        };
    }

    private static String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? "unknown" : value.asText();
    }

    private static String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new UnrecoverableDomainEventException("Missing field: " + field);
        }
        return value.asText();
    }

    private record NotificationContent(String title, String message) {
    }
}
