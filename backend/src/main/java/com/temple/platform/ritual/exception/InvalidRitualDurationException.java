package com.temple.platform.ritual.exception;

public class InvalidRitualDurationException extends RuntimeException {

    public InvalidRitualDurationException() {
        super("Ritual duration must be greater than 0");
    }
}
