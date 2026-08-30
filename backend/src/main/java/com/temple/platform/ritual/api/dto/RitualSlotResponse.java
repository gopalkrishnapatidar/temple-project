package com.temple.platform.ritual.api.dto;

import com.temple.platform.ritual.domain.RitualSlotStatus;

import java.time.Instant;

public record RitualSlotResponse(
        long id,
        long ritualId,
        Instant startAt,
        Instant endAt,
        RitualSlotStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
