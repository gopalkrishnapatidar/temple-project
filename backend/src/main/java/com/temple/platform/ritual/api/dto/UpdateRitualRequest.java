package com.temple.platform.ritual.api.dto;

import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRitualRequest(
        RitualType type,

        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @Min(1)
        Integer durationMinutes,

        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        RitualCurrency currency,

        RitualStatus status
) {
}
