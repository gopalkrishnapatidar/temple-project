package com.temple.platform.availability.api.dto;

public record SlotAvailabilityResponse(
        long slotId,
        int capacity,
        int bookedQuantity,
        int remainingCapacity,
        boolean available
) {
}
