package com.temple.platform.temple.api.dto;

import com.temple.platform.temple.domain.EventStatus;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UpdateTempleEventRequest(
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        OffsetDateTime startAt,

        OffsetDateTime endAt,

        EventStatus status
) {
}
