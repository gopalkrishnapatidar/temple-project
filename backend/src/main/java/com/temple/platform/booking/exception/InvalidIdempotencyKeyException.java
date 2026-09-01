package com.temple.platform.booking.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key header is required");
    }
}
