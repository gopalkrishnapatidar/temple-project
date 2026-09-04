package com.temple.platform.donation.exception;

public class InvalidDonationStateTransitionException extends RuntimeException {

    public InvalidDonationStateTransitionException() {
        super("Invalid donation state transition");
    }
}
