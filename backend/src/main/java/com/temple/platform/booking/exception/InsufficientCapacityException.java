package com.temple.platform.booking.exception;

public class InsufficientCapacityException extends RuntimeException {

    public InsufficientCapacityException() {
        super("Insufficient slot capacity");
    }
}
