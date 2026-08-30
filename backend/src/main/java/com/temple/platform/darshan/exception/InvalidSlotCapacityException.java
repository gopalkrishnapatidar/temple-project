package com.temple.platform.darshan.exception;

public class InvalidSlotCapacityException extends RuntimeException {

    public InvalidSlotCapacityException() {
        super("Slot capacity must be greater than zero");
    }
}
