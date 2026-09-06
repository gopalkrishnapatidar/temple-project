package com.temple.platform.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        long id,
        UUID eventId,
        AggregateType aggregateType,
        String aggregateReference,
        DomainEventType eventType,
        int eventVersion,
        String payloadJson,
        Instant createdAt,
        Instant publishedAt,
        int publishAttempts,
        String lastError
) {
}
