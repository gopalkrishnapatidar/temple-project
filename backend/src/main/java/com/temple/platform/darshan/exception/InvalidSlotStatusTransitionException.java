package com.temple.platform.darshan.exception;

public class InvalidSlotStatusTransitionException extends RuntimeException {

    public InvalidSlotStatusTransitionException() {
        super("Invalid slot status transition");
    }
}
