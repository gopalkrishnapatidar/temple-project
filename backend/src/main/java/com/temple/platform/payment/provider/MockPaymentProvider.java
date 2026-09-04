package com.temple.platform.payment.provider;

import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private final Map<UUID, ProviderInitiationResult> initiations = new ConcurrentHashMap<>();
    private final Map<String, PaymentStatus> providerStates = new ConcurrentHashMap<>();

    @Override
    public ProviderInitiationResult initiate(ProviderInitiationRequest request) {
        return initiations.computeIfAbsent(request.paymentReference(), paymentReference -> {
            String providerReference = providerReferenceFor(paymentReference);
            PaymentStatus initialStatus = resolveInitialStatus(request.amount());
            providerStates.put(providerReference, initialStatus);
            return new ProviderInitiationResult(providerReference, initialStatus);
        });
    }

    @Override
    public Optional<ProviderStatusResult> getStatus(String providerReference) {
        PaymentStatus status = providerStates.get(providerReference);
        if (status == null) {
            return Optional.empty();
        }
        return Optional.of(new ProviderStatusResult(providerReference, status));
    }

    public void setProviderStatus(String providerReference, PaymentStatus status) {
        providerStates.put(providerReference, status);
    }

    static String providerReferenceFor(UUID paymentReference) {
        return "mock_" + paymentReference;
    }

    static PaymentStatus resolveInitialStatus(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        int cents = normalized.remainder(BigDecimal.ONE).movePointRight(2).intValueExact();
        if (cents == 99) {
            return PaymentStatus.FAILED;
        }
        if (cents == 50) {
            return PaymentStatus.PENDING;
        }
        return PaymentStatus.SUCCEEDED;
    }
}
