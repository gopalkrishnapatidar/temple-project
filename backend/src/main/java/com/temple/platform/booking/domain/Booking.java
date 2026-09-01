package com.temple.platform.booking.domain;

import java.time.Instant;
import java.util.UUID;

public record Booking(
        long id,
        UUID bookingReference,
        long accountId,
        Long darshanSlotId,
        Long ritualSlotId,
        int quantity,
        BookingStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
    public BookingTargetType targetType() {
        return darshanSlotId != null ? BookingTargetType.DARSHAN : BookingTargetType.RITUAL;
    }

    public long slotId() {
        return darshanSlotId != null ? darshanSlotId : ritualSlotId;
    }
}
