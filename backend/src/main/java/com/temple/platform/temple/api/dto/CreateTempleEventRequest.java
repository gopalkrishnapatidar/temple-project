package com.temple.platform.temple.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateTempleEventRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        OffsetDateTime startAt,

        @NotNull
        OffsetDateTime endAt
) {
}
