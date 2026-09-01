package com.temple.platform.ritual.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateRitualSlotRequest(
        @NotNull
        Instant startAt,

        @NotNull
        Instant endAt,

        @NotNull
        @Min(1)
        Integer capacity
) {
}
