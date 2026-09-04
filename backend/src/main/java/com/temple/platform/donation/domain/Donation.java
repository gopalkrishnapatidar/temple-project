package com.temple.platform.donation.domain;

import com.temple.platform.payment.domain.PaymentCurrency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Donation(
        long id,
        UUID donationReference,
        long templeId,
        long accountId,
        BigDecimal amount,
        PaymentCurrency currency,
        DonationStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
}
