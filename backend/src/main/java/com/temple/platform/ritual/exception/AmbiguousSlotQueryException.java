package com.temple.platform.ritual.exception;

public class AmbiguousSlotQueryException extends RuntimeException {

    public AmbiguousSlotQueryException() {
        super("Cannot combine date with from or to query parameters");
    }
}
