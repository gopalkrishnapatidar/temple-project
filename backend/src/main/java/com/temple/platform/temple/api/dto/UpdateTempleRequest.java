package com.temple.platform.temple.api.dto;

import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.validation.ValidTimezone;
import jakarta.validation.constraints.Size;

public record UpdateTempleRequest(
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String country,

        @Size(max = 64)
        @ValidTimezone
        String timezone,

        TempleStatus status
) {
}
