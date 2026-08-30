package com.temple.platform.ritual.api.dto;

import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;

import java.math.BigDecimal;
import java.time.Instant;

public record RitualResponse(
        long id,
        long templeId,
        RitualType type,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        RitualCurrency currency,
        RitualStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
