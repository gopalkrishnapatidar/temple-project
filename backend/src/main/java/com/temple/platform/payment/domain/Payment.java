package com.temple.platform.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        long id,
        UUID paymentReference,
        long accountId,
        PaymentPurpose purpose,
        Long bookingId,
        Long donationId,
        BigDecimal amount,
        PaymentCurrency currency,
        PaymentStatus status,
        String providerReference,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
}
