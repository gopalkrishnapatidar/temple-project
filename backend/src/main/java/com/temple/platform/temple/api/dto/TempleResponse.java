package com.temple.platform.temple.api.dto;

import com.temple.platform.temple.domain.TempleStatus;

import java.time.OffsetDateTime;

public record TempleResponse(
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
