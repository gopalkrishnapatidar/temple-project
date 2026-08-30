package com.temple.platform.ritual.domain;

import java.time.Instant;

public record RitualSlot(
        long id,
        long ritualId,
        Instant startAt,
        Instant endAt,
        RitualSlotStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
