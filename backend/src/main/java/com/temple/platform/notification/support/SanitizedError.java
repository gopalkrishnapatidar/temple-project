package com.temple.platform.notification.support;

public final class SanitizedError {

    private static final int MAX_LENGTH = 512;

    private SanitizedError() {
    }

    public static String from(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        message = message.replaceAll("[\\r\\n\\t]", " ").trim();
        if (message.length() > MAX_LENGTH) {
            return message.substring(0, MAX_LENGTH);
        }
        return message;
    }
}
