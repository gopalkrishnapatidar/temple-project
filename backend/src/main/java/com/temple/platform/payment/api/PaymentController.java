package com.temple.platform.payment.api;

import com.temple.platform.payment.api.dto.PaymentResponse;
import com.temple.platform.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentReference}")
    public PaymentResponse get(
            @PathVariable UUID paymentReference,
            Authentication authentication) {
        return paymentService.getPayment(paymentReference, authentication);
    }

    @PostMapping("/{paymentReference}/reconcile")
    public PaymentResponse reconcile(
            @PathVariable UUID paymentReference,
            Authentication authentication) {
        return paymentService.reconcile(paymentReference, authentication);
    }
}
