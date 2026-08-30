package com.temple.platform.temple.domain;

import java.time.OffsetDateTime;

public record TempleEvent(
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
