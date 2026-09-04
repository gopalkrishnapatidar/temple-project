package com.temple.platform.payment.api.dto;

import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentPurpose;
import com.temple.platform.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentReference,
        PaymentPurpose purpose,
        Long bookingId,
        Long donationId,
        BigDecimal amount,
        PaymentCurrency currency,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt
) {
}
