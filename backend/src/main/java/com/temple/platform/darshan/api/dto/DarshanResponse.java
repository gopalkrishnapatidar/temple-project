package com.temple.platform.darshan.api.dto;

import com.temple.platform.darshan.domain.DarshanStatus;

import java.time.OffsetDateTime;

public record DarshanResponse(
        long id,
        long templeId,
        String name,
        String description,
        DarshanStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
