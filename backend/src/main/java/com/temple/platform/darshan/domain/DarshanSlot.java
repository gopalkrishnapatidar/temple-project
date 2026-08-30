package com.temple.platform.darshan.domain;

import java.time.OffsetDateTime;

public record DarshanSlot(
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
