package com.temple.platform.payment.api;

import com.temple.platform.payment.api.dto.PaymentResponse;
import com.temple.platform.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingPaymentController {

    private final PaymentService paymentService;

    public BookingPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{bookingReference}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse initiateBookingPayment(
            @PathVariable UUID bookingReference,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        return paymentService.initiateBookingPayment(bookingReference, idempotencyKey, authentication);
    }
}
