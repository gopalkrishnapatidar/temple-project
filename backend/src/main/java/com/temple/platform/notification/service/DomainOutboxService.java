package com.temple.platform.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.booking.domain.Booking;
import com.temple.platform.notification.domain.AggregateType;
import com.temple.platform.notification.domain.DomainEvent;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.repository.OutboxEventRepository;
import com.temple.platform.payment.domain.Payment;
import com.temple.platform.payment.domain.PaymentPurpose;
import com.temple.platform.payment.domain.PaymentStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DomainOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DomainOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void enqueueBookingConfirmed(Booking booking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", booking.accountId());
        payload.put("bookingReference", booking.bookingReference().toString());
        payload.put("targetType", booking.targetType().name());
        payload.put("quantity", booking.quantity());
        enqueue(
                AggregateType.BOOKING,
                booking.bookingReference().toString(),
                DomainEventType.BOOKING_CONFIRMED,
                payload
        );
    }

    public void enqueueBookingCancelled(Booking booking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", booking.accountId());
        payload.put("bookingReference", booking.bookingReference().toString());
        payload.put("targetType", booking.targetType().name());
        payload.put("quantity", booking.quantity());
        enqueue(
                AggregateType.BOOKING,
                booking.bookingReference().toString(),
                DomainEventType.BOOKING_CANCELLED,
                payload
        );
    }

    public void enqueuePaymentStatusChange(Payment payment) {
        DomainEventType eventType = payment.status() == PaymentStatus.SUCCEEDED
                ? DomainEventType.PAYMENT_SUCCEEDED
                : DomainEventType.PAYMENT_FAILED;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", payment.accountId());
        payload.put("paymentReference", payment.paymentReference().toString());
        payload.put("purpose", payment.purpose().name());
        payload.put("amount", payment.amount().toPlainString());
        payload.put("currency", payment.currency().name());
        if (payment.purpose() == PaymentPurpose.BOOKING && payment.bookingId() != null) {
            payload.put("bookingId", payment.bookingId());
        }
        if (payment.purpose() == PaymentPurpose.DONATION && payment.donationId() != null) {
            payload.put("donationId", payment.donationId());
        }
        enqueue(
                AggregateType.PAYMENT,
                payment.paymentReference().toString(),
                eventType,
                payload
        );
    }

    private void enqueue(
            AggregateType aggregateType,
            String aggregateReference,
            DomainEventType eventType,
            Map<String, Object> payload) {
        Instant occurredAt = clock.instant();
        UUID eventId = UUID.randomUUID();
        DomainEvent envelope = new DomainEvent(
                eventId,
                eventType,
                DomainEvent.CURRENT_VERSION,
                occurredAt,
                aggregateReference,
                payload
        );
        String payloadJson = serializeEnvelope(envelope);
        outboxEventRepository.insert(
                eventId,
                aggregateType,
                aggregateReference,
                eventType,
                DomainEvent.CURRENT_VERSION,
                payloadJson
        );
    }

    private String serializeEnvelope(DomainEvent envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize domain event", ex);
        }
    }
}
