package com.temple.platform.booking.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key already used for a different booking");
    }
}
