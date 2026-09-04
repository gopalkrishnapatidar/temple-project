package com.temple.platform.payment.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException() {
        super("Invalid webhook signature");
    }
}
