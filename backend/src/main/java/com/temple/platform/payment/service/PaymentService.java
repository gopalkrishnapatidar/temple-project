package com.temple.platform.payment.service;

import com.temple.platform.booking.domain.Booking;
import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.domain.BookingTargetType;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.donation.domain.Donation;
import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.donation.repository.DonationRepository;
import com.temple.platform.donation.service.DonationStateMachine;
import com.temple.platform.payment.api.dto.MockWebhookRequest;
import com.temple.platform.payment.api.dto.PaymentResponse;
import com.temple.platform.payment.domain.Payment;
import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentPurpose;
import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.exception.BookingPaymentNotSupportedException;
import com.temple.platform.payment.exception.IdempotencyConflictException;
import com.temple.platform.payment.exception.InvalidIdempotencyKeyException;
import com.temple.platform.payment.exception.PaymentConflictException;
import com.temple.platform.payment.provider.PaymentProvider;
import com.temple.platform.payment.provider.ProviderInitiationRequest;
import com.temple.platform.payment.provider.ProviderInitiationResult;
import com.temple.platform.payment.provider.ProviderStatusResult;
import com.temple.platform.payment.repository.PaymentRepository;
import com.temple.platform.payment.repository.PaymentWebhookEventRepository;
import com.temple.platform.payment.webhook.WebhookSignatureVerifier;
import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final BookingRepository bookingRepository;
    private final DonationRepository donationRepository;
    private final RitualSlotRepository ritualSlotRepository;
    private final RitualRepository ritualRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final TempleAuthorizationService authorizationService;
    private final PaymentProvider paymentProvider;
    private final WebhookSignatureVerifier webhookSignatureVerifier;
    private final TransactionTemplate transactionTemplate;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentWebhookEventRepository webhookEventRepository,
            BookingRepository bookingRepository,
            DonationRepository donationRepository,
            RitualSlotRepository ritualSlotRepository,
            RitualRepository ritualRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            TempleAuthorizationService authorizationService,
            PaymentProvider paymentProvider,
            WebhookSignatureVerifier webhookSignatureVerifier,
            TransactionTemplate transactionTemplate) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.bookingRepository = bookingRepository;
        this.donationRepository = donationRepository;
        this.ritualSlotRepository = ritualSlotRepository;
        this.ritualRepository = ritualRepository;
        this.assignmentRepository = assignmentRepository;
        this.authorizationService = authorizationService;
        this.paymentProvider = paymentProvider;
        this.webhookSignatureVerifier = webhookSignatureVerifier;
        this.transactionTemplate = transactionTemplate;
    }

    public PaymentResponse initiateBookingPayment(
            UUID bookingReference,
            String idempotencyKey,
            Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        String key = requireIdempotencyKey(idempotencyKey);
        Payment pending = transactionTemplate.execute(status -> prepareBookingPayment(bookingReference, accountId, key));
        if (pending.providerReference() != null) {
            return toResponse(pending);
        }
        return initiateWithProvider(pending);
    }

    public PaymentResponse initiateDonationPayment(Donation donation, String paymentIdempotencyKey) {
        Payment pending = transactionTemplate.execute(status -> prepareDonationPayment(donation, paymentIdempotencyKey));
        if (pending.providerReference() != null) {
            return toResponse(pending);
        }
        return initiateWithProvider(pending);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentReference, Authentication authentication) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!canAccess(payment, authentication)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return toResponse(payment);
    }

    public PaymentResponse reconcile(UUID paymentReference, Authentication authentication) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!canAccess(payment, authentication)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        if (PaymentStateMachine.isTerminal(payment.status())) {
            return toResponse(payment);
        }
        if (payment.providerReference() == null) {
            return toResponse(payment);
        }
        ProviderStatusResult providerStatus = paymentProvider.getStatus(payment.providerReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return transactionTemplate.execute(status -> applyProviderStatus(payment, providerStatus.status()));
    }

    public void processWebhook(byte[] rawBody, String signatureHeader, MockWebhookRequest request) {
        webhookSignatureVerifier.verify(rawBody, signatureHeader);
        transactionTemplate.executeWithoutResult(status -> applyWebhookEvent(request));
    }

    private Payment prepareBookingPayment(UUID bookingReference, long accountId, String key) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.accountId() != accountId) {
            throw new ResourceNotFoundException("Booking not found");
        }
        if (booking.status() != BookingStatus.CONFIRMED) {
            throw new PaymentConflictException("Only confirmed bookings can be paid");
        }
        Optional<Payment> existing = paymentRepository.findByAccountIdAndIdempotencyKey(accountId, key);
        if (existing.isPresent()) {
            return replayOrConflictBooking(existing.get(), booking.id());
        }
        Optional<Payment> activePayment = paymentRepository.findActiveByBookingId(booking.id());
        if (activePayment.isPresent()) {
            throw new PaymentConflictException("An active payment already exists for this booking");
        }
        AmountDerivation amount = deriveBookingAmount(booking);
        Optional<Payment> inserted;
        try {
            inserted = paymentRepository.insertIgnoringIdempotencyConflict(
                    UUID.randomUUID(),
                    accountId,
                    PaymentPurpose.BOOKING,
                    booking.id(),
                    null,
                    amount.amount(),
                    amount.currency(),
                    PaymentStatus.PENDING,
                    null,
                    key
            );
        } catch (DataIntegrityViolationException ex) {
            throw new PaymentConflictException("An active payment already exists for this booking");
        }
        if (inserted.isPresent()) {
            return inserted.get();
        }
        Optional<Payment> concurrent = paymentRepository.findByAccountIdAndIdempotencyKey(accountId, key);
        if (concurrent.isPresent()) {
            return replayOrConflictBooking(concurrent.get(), booking.id());
        }
        throw new PaymentConflictException("An active payment already exists for this booking");
    }

    private Payment prepareDonationPayment(Donation donation, String paymentIdempotencyKey) {
        Optional<Payment> existing = paymentRepository.findByAccountIdAndIdempotencyKey(
                donation.accountId(),
                paymentIdempotencyKey
        );
        if (existing.isPresent()) {
            if (!existing.get().donationId().equals(donation.id())) {
                throw new IdempotencyConflictException();
            }
            return existing.get();
        }
        Optional<Payment> inserted = paymentRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                donation.accountId(),
                PaymentPurpose.DONATION,
                null,
                donation.id(),
                donation.amount(),
                donation.currency(),
                PaymentStatus.PENDING,
                null,
                paymentIdempotencyKey
        );
        if (inserted.isPresent()) {
            return inserted.get();
        }
        Payment concurrent = paymentRepository.findByAccountIdAndIdempotencyKey(
                donation.accountId(),
                paymentIdempotencyKey
        ).orElseThrow(() -> new IllegalStateException("Payment not found after idempotency conflict"));
        if (!concurrent.donationId().equals(donation.id())) {
            throw new IdempotencyConflictException();
        }
        return concurrent;
    }

    private PaymentResponse initiateWithProvider(Payment payment) {
        ProviderInitiationResult providerResult = paymentProvider.initiate(
                new ProviderInitiationRequest(
                        payment.paymentReference(),
                        payment.amount(),
                        payment.currency()
                )
        );
        return transactionTemplate.execute(status -> {
            paymentRepository.updateProviderReferenceAndStatus(
                    payment.id(),
                    providerResult.providerReference(),
                    providerResult.initialStatus()
            );
            Payment updated = paymentRepository.findById(payment.id()).orElseThrow();
            syncDonationStatus(updated);
            return toResponse(updated);
        });
    }

    private PaymentResponse applyWebhookEvent(MockWebhookRequest request) {
        boolean firstDelivery = webhookEventRepository.insertIfAbsent(
                request.providerEventId(),
                request.providerReference(),
                request.status()
        );
        Payment payment = paymentRepository.findByProviderReference(request.providerReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!firstDelivery) {
            return toResponse(payment);
        }
        return applyProviderStatus(payment, request.status());
    }

    private PaymentResponse applyProviderStatus(Payment payment, PaymentStatus targetStatus) {
        PaymentStateMachine.requireTransition(payment.status(), targetStatus);
        if (payment.status() != targetStatus) {
            paymentRepository.updateStatus(payment.id(), targetStatus);
        }
        Payment updated = paymentRepository.findById(payment.id()).orElseThrow();
        syncDonationStatus(updated);
        return toResponse(updated);
    }

    private void syncDonationStatus(Payment payment) {
        if (payment.purpose() != PaymentPurpose.DONATION || payment.donationId() == null) {
            return;
        }
        Donation donation = donationRepository.findById(payment.donationId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        DonationStatus target = switch (payment.status()) {
            case SUCCEEDED -> DonationStatus.COMPLETED;
            case FAILED -> DonationStatus.FAILED;
            case PENDING -> DonationStatus.PENDING;
        };
        DonationStateMachine.requireTransition(donation.status(), target);
        if (donation.status() != target) {
            donationRepository.updateStatus(donation.id(), target);
        }
    }

    private AmountDerivation deriveBookingAmount(Booking booking) {
        if (booking.targetType() == BookingTargetType.DARSHAN) {
            throw new BookingPaymentNotSupportedException();
        }
        long ritualSlotId = booking.ritualSlotId();
        var slot = ritualSlotRepository.findById(ritualSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Ritual ritual = ritualRepository.findById(slot.ritualId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        BigDecimal unitPrice = ritual.price().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(booking.quantity()))
                .setScale(2, RoundingMode.UNNECESSARY);
        return new AmountDerivation(total, PaymentCurrency.INR);
    }

    private Payment replayOrConflictBooking(Payment existing, long bookingId) {
        if (existing.purpose() != PaymentPurpose.BOOKING || !bookingIdEquals(existing.bookingId(), bookingId)) {
            throw new IdempotencyConflictException();
        }
        return existing;
    }

    private boolean bookingIdEquals(Long paymentBookingId, long bookingId) {
        return paymentBookingId != null && paymentBookingId == bookingId;
    }

    private boolean canAccess(Payment payment, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        if (payment.accountId() == accountId) {
            return true;
        }
        if (authorizationService.isPlatformAdmin(authentication)) {
            return true;
        }
        if (authorizationService.isTempleAdmin(authentication)) {
            Long templeId = paymentRepository.findTempleIdByPaymentId(payment.id()).orElse(null);
            return templeId != null && assignmentRepository.exists(accountId, templeId);
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

    static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.paymentReference(),
                payment.purpose(),
                payment.bookingId(),
                payment.donationId(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.providerReference(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }

    private record AmountDerivation(BigDecimal amount, PaymentCurrency currency) {
    }
}
