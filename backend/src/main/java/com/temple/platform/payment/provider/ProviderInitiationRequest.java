package com.temple.platform.payment.provider;

import com.temple.platform.payment.domain.PaymentCurrency;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderInitiationRequest(
        UUID paymentReference,
        BigDecimal amount,
        PaymentCurrency currency
) {
}
