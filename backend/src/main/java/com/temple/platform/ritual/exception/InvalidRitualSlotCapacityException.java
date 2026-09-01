package com.temple.platform.ritual.exception;

public class InvalidRitualSlotCapacityException extends RuntimeException {

    public InvalidRitualSlotCapacityException() {
        super("Slot capacity must be greater than zero");
    }
}
