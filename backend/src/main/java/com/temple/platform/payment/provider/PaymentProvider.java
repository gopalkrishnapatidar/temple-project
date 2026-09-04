package com.temple.platform.payment.provider;

import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentProvider {

    ProviderInitiationResult initiate(ProviderInitiationRequest request);

    Optional<ProviderStatusResult> getStatus(String providerReference);
}
