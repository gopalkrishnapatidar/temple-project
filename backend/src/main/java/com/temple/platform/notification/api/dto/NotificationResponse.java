package com.temple.platform.notification.api.dto;

import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.NotificationChannel;
import com.temple.platform.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationReference,
        NotificationChannel channel,
        DomainEventType type,
        NotificationStatus status,
        String title,
        String message,
        Instant createdAt,
        Instant sentAt
) {
}
