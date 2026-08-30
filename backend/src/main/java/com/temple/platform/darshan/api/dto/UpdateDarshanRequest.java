package com.temple.platform.darshan.api.dto;

import com.temple.platform.darshan.domain.DarshanStatus;
import jakarta.validation.constraints.Size;

public record UpdateDarshanRequest(
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        DarshanStatus status
) {
}
