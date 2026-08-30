package com.temple.platform.darshan.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateDarshanSlotRequest(
        @NotNull
        OffsetDateTime startAt,

        @NotNull
        OffsetDateTime endAt,

        @NotNull
        @Min(1)
        Integer capacity
) {
}
