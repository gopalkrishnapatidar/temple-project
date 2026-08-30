package com.temple.platform.ritual.exception;

public class InvalidRitualPriceException extends RuntimeException {

    public InvalidRitualPriceException() {
        super("Ritual price must not be negative");
    }
}
