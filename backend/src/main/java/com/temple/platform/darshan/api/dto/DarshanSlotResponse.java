package com.temple.platform.darshan.api.dto;

import com.temple.platform.darshan.domain.DarshanSlotStatus;

import java.time.OffsetDateTime;

public record DarshanSlotResponse(
        long id,
        long darshanId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int capacity,
        DarshanSlotStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
