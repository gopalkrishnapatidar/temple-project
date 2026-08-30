package com.temple.platform.ritual.exception;

public class InvalidRitualNameException extends RuntimeException {

    public InvalidRitualNameException() {
        super("Ritual name must not be blank");
    }
}
