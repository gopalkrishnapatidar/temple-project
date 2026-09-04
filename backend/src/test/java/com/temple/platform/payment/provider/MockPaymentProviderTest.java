package com.temple.platform.payment.provider;

import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentProviderTest {

    private MockPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockPaymentProvider();
    }

    @Test
    void resolveInitialStatusEndingIn99IsFailed() {
        assertThat(MockPaymentProvider.resolveInitialStatus(new BigDecimal("100.99")))
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(MockPaymentProvider.resolveInitialStatus(new BigDecimal("10.99")))
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void resolveInitialStatusEndingIn50IsPending() {
        assertThat(MockPaymentProvider.resolveInitialStatus(new BigDecimal("100.50")))
                .isEqualTo(PaymentStatus.PENDING);
        assertThat(MockPaymentProvider.resolveInitialStatus(new BigDecimal("10.50")))
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void resolveInitialStatusOtherwiseIsSucceeded() {
        assertThat(MockPaymentProvider.resolveInitialStatus(new BigDecimal("100.00")))
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void repeatedInitiateReturnsSameProviderReference() {
        UUID paymentReference = UUID.randomUUID();
        ProviderInitiationRequest request = new ProviderInitiationRequest(
                paymentReference,
                new BigDecimal("25.00"),
                PaymentCurrency.INR
        );

        ProviderInitiationResult first = provider.initiate(request);
        ProviderInitiationResult second = provider.initiate(request);

        assertThat(second.providerReference()).isEqualTo(first.providerReference());
        assertThat(second.providerReference()).isEqualTo(MockPaymentProvider.providerReferenceFor(paymentReference));
        assertThat(second.initialStatus()).isEqualTo(first.initialStatus());
    }

    @Test
    void differentPaymentsGetDifferentProviderReferences() {
        ProviderInitiationResult first = provider.initiate(new ProviderInitiationRequest(
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                PaymentCurrency.INR
        ));
        ProviderInitiationResult second = provider.initiate(new ProviderInitiationRequest(
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                PaymentCurrency.INR
        ));

        assertThat(second.providerReference()).isNotEqualTo(first.providerReference());
    }
}
