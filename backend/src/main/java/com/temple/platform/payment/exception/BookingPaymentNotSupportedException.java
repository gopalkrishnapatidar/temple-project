package com.temple.platform.payment.exception;

public class BookingPaymentNotSupportedException extends RuntimeException {

    public BookingPaymentNotSupportedException() {
        super("Payment is not supported for this booking type because no authoritative price is configured");
    }
}
