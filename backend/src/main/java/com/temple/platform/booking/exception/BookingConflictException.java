package com.temple.platform.booking.exception;

public class BookingConflictException extends RuntimeException {

    public BookingConflictException() {
        super("Slot is not available for booking");
    }
}
