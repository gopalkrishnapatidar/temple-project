package com.temple.platform.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        long id,
        UUID notificationReference,
        long accountId,
        NotificationChannel channel,
        DomainEventType type,
        NotificationStatus status,
        String title,
        String message,
        UUID sourceEventId,
        Instant createdAt,
        Instant sentAt,
        String failureReason
) {
}
