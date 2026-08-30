package com.temple.platform.darshan.domain;

import java.time.OffsetDateTime;

public record Darshan(
        long id,
        long templeId,
        String name,
        String description,
        DarshanStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
