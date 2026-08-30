package com.temple.platform.darshan.api.dto;

import com.temple.platform.darshan.domain.DarshanSlotStatus;
import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;

public record UpdateDarshanSlotRequest(
        OffsetDateTime startAt,

        OffsetDateTime endAt,

        @Min(1)
        Integer capacity,

        DarshanSlotStatus status
) {
}
