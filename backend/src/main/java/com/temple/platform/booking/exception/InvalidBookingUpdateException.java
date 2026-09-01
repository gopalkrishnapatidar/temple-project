package com.temple.platform.booking.exception;

public class InvalidBookingUpdateException extends RuntimeException {

    public InvalidBookingUpdateException() {
        super("Booking can only be updated to CANCELLED");
    }
}
