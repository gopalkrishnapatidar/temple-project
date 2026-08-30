package com.temple.platform.ritual.api.dto;

import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRitualRequest(
        @NotNull
        RitualType type,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @Min(1)
        Integer durationMinutes,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        RitualCurrency currency
) {
}
