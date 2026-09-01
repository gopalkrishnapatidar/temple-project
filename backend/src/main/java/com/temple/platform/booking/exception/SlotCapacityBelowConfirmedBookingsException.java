package com.temple.platform.booking.exception;

public class SlotCapacityBelowConfirmedBookingsException extends RuntimeException {

    public SlotCapacityBelowConfirmedBookingsException() {
        super("Slot capacity cannot be reduced below confirmed booking quantity");
    }
}
