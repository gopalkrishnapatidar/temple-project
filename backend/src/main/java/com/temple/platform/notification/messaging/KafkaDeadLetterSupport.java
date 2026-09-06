package com.temple.platform.notification.messaging;

import org.apache.kafka.common.TopicPartition;

public final class KafkaDeadLetterSupport {

    private KafkaDeadLetterSupport() {
    }

    public static TopicPartition dltPartition(String dltTopic) {
        return new TopicPartition(dltTopic, 0);
    }
}
