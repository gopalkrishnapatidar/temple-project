package com.temple.platform.darshan.exception;

public class OverlappingSlotException extends RuntimeException {

    public OverlappingSlotException() {
        super("Darshan slot time range overlaps an existing available slot");
    }
}
