package com.temple.platform.darshan.exception;

public class InvalidSlotScheduleException extends RuntimeException {

    public InvalidSlotScheduleException() {
        super("Slot end time must be after start time");
    }
}
