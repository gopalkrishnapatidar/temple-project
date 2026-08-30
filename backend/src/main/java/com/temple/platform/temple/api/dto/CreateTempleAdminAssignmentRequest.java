package com.temple.platform.temple.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTempleAdminAssignmentRequest(
        @NotNull
        @Positive
        Long accountId
) {
}
