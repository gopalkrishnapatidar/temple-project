package com.temple.platform.notification.messaging;

public class KafkaUnavailableException extends RuntimeException {

    public KafkaUnavailableException(Throwable cause) {
        super("Kafka is unavailable", cause);
    }
}
