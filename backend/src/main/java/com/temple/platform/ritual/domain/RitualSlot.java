package com.temple.platform.ritual.domain;

import java.time.Instant;

public record RitualSlot(
        long id,
        long ritualId,
        Instant startAt,
        Instant endAt,
        int capacity,
        RitualSlotStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
