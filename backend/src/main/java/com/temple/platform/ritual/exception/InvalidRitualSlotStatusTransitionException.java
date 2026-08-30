package com.temple.platform.ritual.exception;

public class InvalidRitualSlotStatusTransitionException extends RuntimeException {

    public InvalidRitualSlotStatusTransitionException() {
        super("Invalid slot status transition");
    }
}
