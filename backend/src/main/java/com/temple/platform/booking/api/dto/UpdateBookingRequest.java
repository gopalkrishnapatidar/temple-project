package com.temple.platform.booking.api.dto;

import com.temple.platform.booking.domain.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingRequest(
        @NotNull
        BookingStatus status
) {
}
