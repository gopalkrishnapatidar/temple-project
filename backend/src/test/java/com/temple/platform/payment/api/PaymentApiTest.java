package com.temple.platform.payment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.donation.repository.DonationRepository;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.provider.MockPaymentProvider;
import com.temple.platform.payment.repository.PaymentRepository;
import com.temple.platform.payment.repository.PaymentWebhookEventRepository;
import com.temple.platform.payment.webhook.WebhookSignatureVerifier;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class PaymentApiTest {

    private static final String PASSWORD = "ValidPass1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private DarshanRepository darshanRepository;

    @Autowired
    private DarshanSlotRepository darshanSlotRepository;

    @Autowired
    private RitualRepository ritualRepository;

    @Autowired
    private RitualSlotRepository ritualSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private MockPaymentProvider mockPaymentProvider;

    @Autowired
    private WebhookSignatureVerifier webhookSignatureVerifier;

    @Test
    void ritualBookingPaymentDerivesServerSideAmount() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("25.00"), 4);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 2);
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("BOOKING"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void sameIdempotencyKeyRetriesBookingPayment() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key))
                .andExpect(status().isCreated())
                .andReturn();
        String firstReference = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("paymentReference").asText();

        mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value(firstReference));
    }

    @Test
    void darshanBookingPaymentIsRejected() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "DARSHAN", slotId, 1);

        mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingPaymentBolaReturns404() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 2);
        String owner = registerDevotee();
        String other = registerDevotee();
        String bookingReference = createBooking(owner, "RITUAL", slotId, 1);

        mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + other)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentStatusRequiresAuthorization() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 2);
        String owner = registerDevotee();
        String other = registerDevotee();
        String bookingReference = createBooking(owner, "RITUAL", slotId, 1);
        String paymentReference = initiateBookingPayment(owner, bookingReference);

        mockMvc.perform(get("/api/v1/payments/" + paymentReference)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payments/" + paymentReference)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void webhookAcceptsValidSignatureAndCompletesPendingPayment() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        MvcResult paymentResult = mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        JsonNode payment = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        String providerReference = payment.get("providerReference").asText();

        String payload = webhookJson("evt-success-1", providerReference, "SUCCEEDED");
        byte[] raw = payload.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", webhookSignatureVerifier.sign(raw))
                        .content(raw))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/payments/" + payment.get("paymentReference").asText())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void webhookRejectsInvalidSignature() throws Exception {
        String payload = webhookJson("evt-bad-sig", "mock_missing", "SUCCEEDED");
        byte[] raw = payload.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", "sha256=deadbeef")
                        .content(raw))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateWebhookEventIsIdempotent() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        JsonNode payment = objectMapper.readTree(mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String providerReference = payment.get("providerReference").asText();
        String payload = webhookJson("evt-dup-1", providerReference, "SUCCEEDED");
        byte[] raw = payload.getBytes(StandardCharsets.UTF_8);
        String signature = webhookSignatureVerifier.sign(raw);

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", signature)
                        .content(raw))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", signature)
                        .content(raw))
                .andExpect(status().isNoContent());

        assertThat(webhookEventRepository.findEventStatus("evt-dup-1")).isPresent();
    }

    @Test
    void webhookCannotRegressTerminalPayment() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        JsonNode payment = objectMapper.readTree(mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String providerReference = payment.get("providerReference").asText();
        String payload = webhookJson("evt-regress", providerReference, "FAILED");
        byte[] raw = payload.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", webhookSignatureVerifier.sign(raw))
                        .content(raw))
                .andExpect(status().isConflict());
    }

    @Test
    void reconciliationAppliesPendingProviderState() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        JsonNode payment = objectMapper.readTree(mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        mockPaymentProvider.setProviderStatus(payment.get("providerReference").asText(), PaymentStatus.SUCCEEDED);

        mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentReference").asText() + "/reconcile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentReference").asText() + "/reconcile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDifferentIdempotencyKeysForSameBookingProducesConflict() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 5);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            Future<?> one = executor.submit(() -> payConcurrentlyWithVariableStatus(
                    token, bookingReference, UUID.randomUUID().toString(), created, conflicted, ready, go));
            Future<?> two = executor.submit(() -> payConcurrentlyWithVariableStatus(
                    token, bookingReference, UUID.randomUUID().toString(), created, conflicted, ready, go));
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            one.get(15, TimeUnit.SECONDS);
            two.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(1);
        assertThat(conflicted.get()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameIdempotencyKeyCreatesOnePayment() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.00"), 5);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1);
        String key = UUID.randomUUID().toString();
        AtomicInteger created = new AtomicInteger();
        AtomicReference<String> firstReference = new AtomicReference<>();
        AtomicReference<String> secondReference = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            Future<?> one = executor.submit(() -> payConcurrently(token, bookingReference, key, created, firstReference, secondReference, ready, go));
            Future<?> two = executor.submit(() -> payConcurrently(token, bookingReference, key, created, firstReference, secondReference, ready, go));
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            one.get(15, TimeUnit.SECONDS);
            two.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(2);
        assertThat(firstReference.get()).isEqualTo(secondReference.get());
    }

    private void payConcurrentlyWithVariableStatus(
            String token,
            String bookingReference,
            String key,
            AtomicInteger created,
            AtomicInteger conflicted,
            CountDownLatch ready,
            CountDownLatch go) {
        ready.countDown();
        await(go);
        try {
            mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", key))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 201) {
                            created.incrementAndGet();
                        } else if (status == 409) {
                            conflicted.incrementAndGet();
                        } else {
                            throw new AssertionError("Unexpected status " + status);
                        }
                    });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void payConcurrently(
            String token,
            String bookingReference,
            String key,
            AtomicInteger created,
            AtomicReference<String> firstReference,
            AtomicReference<String> secondReference,
            CountDownLatch ready,
            CountDownLatch go) {
        ready.countDown();
        await(go);
        try {
            MvcResult result = mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", key))
                    .andExpect(status().isCreated())
                    .andReturn();
            created.incrementAndGet();
            String reference = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("paymentReference").asText();
            firstReference.compareAndSet(null, reference);
            secondReference.set(reference);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String initiateBookingPayment(String token, String bookingReference) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("paymentReference").asText();
    }

    private long createFutureRitualSlotWithPrice(BigDecimal price, int capacity) {
        long templeId = createTemple();
        long ritualId = ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Puja " + UUID.randomUUID(),
                null,
                30,
                price,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        Instant start = Instant.now().plus(Duration.ofDays(6));
        return ritualSlotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), capacity, RitualSlotStatus.AVAILABLE).id();
    }

    private long createFutureDarshanSlot(int capacity) {
        long templeId = createTemple();
        long darshanId = darshanRepository.insert(templeId, "Darshan " + UUID.randomUUID(), null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        return darshanSlotRepository.insert(darshanId, start, start.plusHours(1), capacity, DarshanSlotStatus.AVAILABLE).id();
    }

    private long createTemple() {
        return templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
    }

    private String createBooking(String token, String targetType, long slotId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(targetType, slotId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("bookingReference").asText();
    }

    private String registerDevotee() throws Exception {
        String email = "devotee-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class).accessToken();
    }

    private static String bookingJson(String targetType, long slotId, int quantity) {
        return """
                {
                  "targetType":"%s",
                  "slotId":%d,
                  "quantity":%d
                }
                """.formatted(targetType, slotId, quantity);
    }

    private static String webhookJson(String eventId, String providerReference, String status) {
        return """
                {
                  "providerEventId":"%s",
                  "providerReference":"%s",
                  "status":"%s"
                }
                """.formatted(eventId, providerReference, status);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
