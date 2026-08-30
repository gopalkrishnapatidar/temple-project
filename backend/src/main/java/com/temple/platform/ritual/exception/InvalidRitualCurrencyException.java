package com.temple.platform.ritual.exception;

public class InvalidRitualCurrencyException extends RuntimeException {

    public InvalidRitualCurrencyException() {
        super("Unsupported currency");
    }
}
