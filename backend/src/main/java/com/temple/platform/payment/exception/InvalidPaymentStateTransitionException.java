package com.temple.platform.payment.exception;

public class InvalidPaymentStateTransitionException extends RuntimeException {

    public InvalidPaymentStateTransitionException() {
        super("Invalid payment state transition");
    }
}
