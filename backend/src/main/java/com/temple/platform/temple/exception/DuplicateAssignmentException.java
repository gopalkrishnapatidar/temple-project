package com.temple.platform.temple.exception;

public class DuplicateAssignmentException extends RuntimeException {

    public DuplicateAssignmentException() {
        super("Temple admin assignment already exists");
    }
}
