package com.temple.platform.payment.provider;

import com.temple.platform.payment.domain.PaymentStatus;

public record ProviderInitiationResult(
        String providerReference,
        PaymentStatus initialStatus
) {
}
