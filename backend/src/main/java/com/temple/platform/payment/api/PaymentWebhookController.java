package com.temple.platform.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.payment.api.dto.MockWebhookRequest;
import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mockWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader("X-Webhook-Signature") String signature) throws Exception {
        MockWebhookRequest request = objectMapper.readValue(rawBody, MockWebhookRequest.class);
        if (request.providerEventId() == null || request.providerEventId().isBlank()
                || request.providerReference() == null || request.providerReference().isBlank()
                || request.status() == null
                || (request.status() != PaymentStatus.SUCCEEDED && request.status() != PaymentStatus.FAILED)) {
            throw new IllegalArgumentException("Invalid webhook payload");
        }
        paymentService.processWebhook(rawBody, signature, request);
    }
}
