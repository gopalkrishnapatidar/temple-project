package com.temple.platform.ritual.api.dto;

import com.temple.platform.ritual.domain.RitualSlotStatus;

import java.time.Instant;

public record UpdateRitualSlotRequest(
        Instant startAt,

        Instant endAt,

        RitualSlotStatus status
) {
}
