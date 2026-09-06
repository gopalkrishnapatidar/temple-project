package com.temple.platform.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        DomainEventType eventType,
        int eventVersion,
        Instant occurredAt,
        String aggregateReference,
        Object payload
) {
    public static final int CURRENT_VERSION = 1;
}
