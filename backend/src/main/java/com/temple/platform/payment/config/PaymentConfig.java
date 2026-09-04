package com.temple.platform.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MockPaymentProperties.class)
public class PaymentConfig {
}
