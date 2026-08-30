package com.temple.platform.ritual.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Ritual(
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
