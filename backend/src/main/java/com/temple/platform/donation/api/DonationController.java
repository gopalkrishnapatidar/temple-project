package com.temple.platform.donation.api;

import com.temple.platform.donation.api.dto.CreateDonationRequest;
import com.temple.platform.donation.api.dto.DonationResponse;
import com.temple.platform.donation.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse create(
            @Valid @RequestBody CreateDonationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        return donationService.create(request, idempotencyKey, authentication);
    }

    @GetMapping("/{donationReference}")
    public DonationResponse get(
            @PathVariable UUID donationReference,
            Authentication authentication) {
        return donationService.get(donationReference, authentication);
    }
}
