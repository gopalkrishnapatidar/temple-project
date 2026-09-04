package com.temple.platform.payment.api.dto;

import com.temple.platform.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String providerReference,
        @NotNull PaymentStatus status
) {
}
