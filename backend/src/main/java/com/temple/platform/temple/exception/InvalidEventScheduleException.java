package com.temple.platform.temple.exception;

public class InvalidEventScheduleException extends RuntimeException {

    public InvalidEventScheduleException() {
        super("Event end time must be after start time");
    }
}
