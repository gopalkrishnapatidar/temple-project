package com.temple.platform.donation.exception;

public class InvalidDonationCurrencyException extends RuntimeException {

    public InvalidDonationCurrencyException() {
        super("Donation currency must be INR");
    }
}
