package com.temple.platform.booking.api.dto;

import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.domain.BookingTargetType;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID bookingReference,
        BookingTargetType targetType,
        long slotId,
        int quantity,
        BookingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
