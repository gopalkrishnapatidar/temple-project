package com.temple.platform.notification.exception;

public class UnrecoverableDomainEventException extends RuntimeException {

    public UnrecoverableDomainEventException(String message) {
        super(message);
    }

    public UnrecoverableDomainEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
