package com.temple.platform.temple.api.dto;

import com.temple.platform.temple.domain.EventStatus;

import java.time.OffsetDateTime;

public record TempleEventResponse(
        long id,
        long templeId,
        String name,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        EventStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
