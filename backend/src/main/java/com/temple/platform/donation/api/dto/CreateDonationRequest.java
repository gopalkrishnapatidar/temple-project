package com.temple.platform.donation.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateDonationRequest(
        @NotNull @Positive Long templeId,
        @NotNull BigDecimal amount,
        @NotNull String currency
) {
}
