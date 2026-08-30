package com.temple.platform.temple.domain;

import java.time.OffsetDateTime;

public record Temple(
        long id,
        String name,
        String description,
        String city,
        String state,
        String country,
        String timezone,
        TempleStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
