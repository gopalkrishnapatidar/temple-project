package com.temple.platform.donation.service;

import com.temple.platform.donation.api.dto.CreateDonationRequest;
import com.temple.platform.donation.api.dto.DonationResponse;
import com.temple.platform.donation.domain.Donation;
import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.donation.exception.IdempotencyConflictException;
import com.temple.platform.donation.exception.InvalidDonationAmountException;
import com.temple.platform.donation.exception.InvalidDonationCurrencyException;
import com.temple.platform.donation.repository.DonationRepository;
import com.temple.platform.payment.api.dto.PaymentResponse;
import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.exception.InvalidIdempotencyKeyException;
import com.temple.platform.payment.repository.PaymentRepository;
import com.temple.platform.payment.service.PaymentService;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonationService {

    static final BigDecimal MAX_DONATION_AMOUNT = new BigDecimal("1000000.00");
    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final DonationRepository donationRepository;
    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final TempleAuthorizationService authorizationService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;

    public DonationService(
            DonationRepository donationRepository,
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            TempleAuthorizationService authorizationService,
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            TransactionTemplate transactionTemplate) {
        this.donationRepository = donationRepository;
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.authorizationService = authorizationService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public DonationResponse create(
            CreateDonationRequest request,
            String idempotencyKey,
            Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        String key = requireIdempotencyKey(idempotencyKey);
        Temple temple = templeRepository.findById(request.templeId())
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (temple.status() != TempleStatus.ACTIVE) {
            throw new ResourceNotFoundException("Temple not found");
        }
        BigDecimal amount = requireDonationAmount(request.amount());
        PaymentCurrency currency = requireDonationCurrency(request.currency());
        Donation donation = transactionTemplate.execute(status ->
                prepareDonation(accountId, key, request.templeId(), amount, currency));
        PaymentResponse payment = paymentService.initiateDonationPayment(donation, "donation-pay-" + key);
        Donation refreshed = donationRepository.findById(donation.id()).orElseThrow();
        return toResponse(refreshed, payment.paymentReference());
    }

    @Transactional(readOnly = true)
    public DonationResponse get(UUID donationReference, Authentication authentication) {
        Donation donation = donationRepository.findByDonationReference(donationReference)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        if (!canAccess(donation, authentication)) {
            throw new ResourceNotFoundException("Donation not found");
        }
        UUID paymentReference = paymentRepository.findPaymentReferenceByDonationId(donation.id()).orElse(null);
        return toResponse(donation, paymentReference);
    }

    private Donation prepareDonation(
            long accountId,
            String key,
            long templeId,
            BigDecimal amount,
            PaymentCurrency currency) {
        Optional<Donation> existing = donationRepository.findByAccountIdAndIdempotencyKey(accountId, key);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), templeId, amount, currency);
        }
        Optional<Donation> inserted = donationRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                templeId,
                accountId,
                amount,
                currency,
                DonationStatus.PENDING,
                key
        );
        if (inserted.isPresent()) {
            return inserted.get();
        }
        Donation concurrent = donationRepository.findByAccountIdAndIdempotencyKey(accountId, key)
                .orElseThrow(() -> new IllegalStateException("Donation not found after idempotency conflict"));
        return replayOrConflict(concurrent, templeId, amount, currency);
    }

    static BigDecimal requireDonationAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDonationAmountException("Donation amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new InvalidDonationAmountException("Donation amount must have at most two decimal places");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (normalized.compareTo(MAX_DONATION_AMOUNT) > 0) {
            throw new InvalidDonationAmountException("Donation amount exceeds the allowed maximum");
        }
        return normalized;
    }

    static PaymentCurrency requireDonationCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidDonationCurrencyException();
        }
        try {
            return PaymentCurrency.valueOf(currency.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidDonationCurrencyException();
        }
    }

    private Donation replayOrConflict(
            Donation existing,
            long templeId,
            BigDecimal amount,
            PaymentCurrency currency) {
        boolean sameRequest = existing.templeId() == templeId
                && existing.amount().compareTo(amount) == 0
                && existing.currency() == currency;
        if (sameRequest) {
            return existing;
        }
        throw new IdempotencyConflictException();
    }

    private boolean canAccess(Donation donation, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        if (donation.accountId() == accountId) {
            return true;
        }
        if (authorizationService.isPlatformAdmin(authentication)) {
            return true;
        }
        if (authorizationService.isTempleAdmin(authentication)) {
            return assignmentRepository.exists(accountId, donation.templeId());
        }
        return false;
    }

    static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException();
        }
        return trimmed;
    }

    private static DonationResponse toResponse(Donation donation, UUID paymentReference) {
        return new DonationResponse(
                donation.donationReference(),
                donation.templeId(),
                donation.amount(),
                donation.currency(),
                donation.status(),
                paymentReference,
                donation.createdAt(),
                donation.updatedAt()
        );
    }
}
