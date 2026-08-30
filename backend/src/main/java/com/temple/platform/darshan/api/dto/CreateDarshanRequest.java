package com.temple.platform.darshan.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDarshanRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description
) {
}
