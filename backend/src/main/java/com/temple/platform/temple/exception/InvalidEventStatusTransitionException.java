package com.temple.platform.temple.exception;

public class InvalidEventStatusTransitionException extends RuntimeException {

    public InvalidEventStatusTransitionException() {
        super("Invalid event status transition");
    }
}
