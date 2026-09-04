package com.temple.platform.payment.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency key is required and must be at most 128 characters");
    }
}
