package com.temple.platform.ritual.exception;

public class InvalidRitualSlotScheduleException extends RuntimeException {

    public InvalidRitualSlotScheduleException() {
        super("Slot end time must be after start time");
    }
}
