package com.temple.platform.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.mock")
public record MockPaymentProperties(
        String webhookSecret
) {
}
