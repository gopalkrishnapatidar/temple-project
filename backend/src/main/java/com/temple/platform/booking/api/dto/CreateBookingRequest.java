package com.temple.platform.booking.api.dto;

import com.temple.platform.booking.domain.BookingTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull
        BookingTargetType targetType,

        @NotNull
        Long slotId,

        @NotNull
        @Min(1)
        @Max(50)
        Integer quantity
) {
}
