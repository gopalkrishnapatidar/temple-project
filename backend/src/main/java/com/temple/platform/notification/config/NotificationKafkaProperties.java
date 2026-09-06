package com.temple.platform.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.notification.kafka")
public class NotificationKafkaProperties {

    private boolean enabled = true;
    private String bootstrapServers = "localhost:9092";
    private String topic = "temple.domain.events";
    private String dltTopic = "temple.domain.events.DLT";
    private String consumerGroup = "temple-notification-consumer";
    private Duration outboxPollInterval = Duration.ofSeconds(5);
    private int outboxBatchSize = 25;
    private int consumerMaxAttempts = 3;
    private Duration consumerBackoff = Duration.ofSeconds(1);
    private Duration producerAckTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDltTopic() {
        return dltTopic;
    }

    public void setDltTopic(String dltTopic) {
        this.dltTopic = dltTopic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Duration getOutboxPollInterval() {
        return outboxPollInterval;
    }

    public void setOutboxPollInterval(Duration outboxPollInterval) {
        this.outboxPollInterval = outboxPollInterval;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public int getConsumerMaxAttempts() {
        return consumerMaxAttempts;
    }

    public void setConsumerMaxAttempts(int consumerMaxAttempts) {
        this.consumerMaxAttempts = consumerMaxAttempts;
    }

    public Duration getConsumerBackoff() {
        return consumerBackoff;
    }

    public void setConsumerBackoff(Duration consumerBackoff) {
        this.consumerBackoff = consumerBackoff;
    }

    public Duration getProducerAckTimeout() {
        return producerAckTimeout;
    }

    public void setProducerAckTimeout(Duration producerAckTimeout) {
        this.producerAckTimeout = producerAckTimeout;
    }
}
