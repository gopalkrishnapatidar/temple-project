package com.temple.platform.temple.api.dto;

import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.validation.ValidTimezone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTempleRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 100)
        String country,

        @NotBlank
        @Size(max = 64)
        @ValidTimezone
        String timezone,

        @NotNull
        TempleStatus status
) {
}
