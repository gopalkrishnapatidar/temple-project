package com.temple.platform.donation.api.dto;

import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.payment.domain.PaymentCurrency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DonationResponse(
        UUID donationReference,
        long templeId,
        BigDecimal amount,
        PaymentCurrency currency,
        DonationStatus status,
        UUID paymentReference,
        Instant createdAt,
        Instant updatedAt
) {
}
