package com.temple.platform.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.provider.MockPaymentProvider;
import com.temple.platform.payment.webhook.WebhookSignatureVerifier;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.support.IsolatedPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IsolatedPostgresIntegrationTest
@AutoConfigureMockMvc
@Transactional
class NotificationOutboxIntegrationTest {

    private static final String PASSWORD = "ValidPass1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    private MockPaymentProvider mockPaymentProvider;

    @Autowired
    private WebhookSignatureVerifier webhookSignatureVerifier;

    @Test
    void bookingConfirmationCreatesOneOutboxEvent() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "DARSHAN", slotId, 1, UUID.randomUUID().toString());

        assertThat(countOutboxEvents("BOOKING_CONFIRMED", bookingReference)).isEqualTo(1);
    }

    @Test
    void idempotentBookingReplayDoesNotCreateAnotherOutboxEvent() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();
        String bookingReference = createBooking(token, "DARSHAN", slotId, 1, key);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(bookingReference));

        assertThat(countOutboxEvents("BOOKING_CONFIRMED", bookingReference)).isEqualTo(1);
    }

    @Test
    void bookingCancellationCreatesOneOutboxEventEvenWhenCancelledTwice() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "DARSHAN", slotId, 1, UUID.randomUUID().toString());

        mockMvc.perform(patch("/api/v1/bookings/" + bookingReference)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/v1/bookings/" + bookingReference)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(countOutboxEvents("BOOKING_CANCELLED", bookingReference)).isEqualTo(1);
        assertThat(countOutboxEvents("BOOKING_CONFIRMED", bookingReference)).isEqualTo(1);
    }

    @Test
    void paymentSucceededTransitionCreatesOneOutboxEvent() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1, UUID.randomUUID().toString());
        JsonNode payment = initiatePendingBookingPayment(token, bookingReference);
        String paymentReference = payment.get("paymentReference").asText();
        String providerReference = payment.get("providerReference").asText();

        assertThat(countOutboxEvents("PAYMENT_SUCCEEDED", paymentReference)).isZero();

        deliverWebhook("evt-outbox-1", providerReference, "SUCCEEDED");

        assertThat(countOutboxEvents("PAYMENT_SUCCEEDED", paymentReference)).isEqualTo(1);
    }

    @Test
    void duplicateWebhookDoesNotCreateAnotherPaymentOutboxEvent() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1, UUID.randomUUID().toString());
        JsonNode payment = initiatePendingBookingPayment(token, bookingReference);
        String paymentReference = payment.get("paymentReference").asText();
        String providerReference = payment.get("providerReference").asText();
        String payload = webhookJson("evt-outbox-dup", providerReference, "SUCCEEDED");
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

        assertThat(countOutboxEvents("PAYMENT_SUCCEEDED", paymentReference)).isEqualTo(1);
    }

    @Test
    void duplicateReconciliationDoesNotCreateAnotherPaymentOutboxEvent() throws Exception {
        long slotId = createFutureRitualSlotWithPrice(new BigDecimal("10.50"), 2);
        String token = registerDevotee();
        String bookingReference = createBooking(token, "RITUAL", slotId, 1, UUID.randomUUID().toString());
        JsonNode payment = initiatePendingBookingPayment(token, bookingReference);
        String paymentReference = payment.get("paymentReference").asText();
        mockPaymentProvider.setProviderStatus(payment.get("providerReference").asText(), PaymentStatus.SUCCEEDED);

        mockMvc.perform(post("/api/v1/payments/" + paymentReference + "/reconcile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        mockMvc.perform(post("/api/v1/payments/" + paymentReference + "/reconcile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        assertThat(countOutboxEvents("PAYMENT_SUCCEEDED", paymentReference)).isEqualTo(1);
    }

    private long countOutboxEvents(String eventType, String aggregateReference) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE event_type = ? AND aggregate_reference = ?
                """,
                Long.class,
                eventType,
                aggregateReference
        );
        return count == null ? 0L : count;
    }

    private JsonNode initiatePendingBookingPayment(String token, String bookingReference) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/bookings/" + bookingReference + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void deliverWebhook(String eventId, String providerReference, String status) throws Exception {
        String payload = webhookJson(eventId, providerReference, status);
        byte[] raw = payload.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", webhookSignatureVerifier.sign(raw))
                        .content(raw))
                .andExpect(status().isNoContent());
    }

    private String createBooking(String token, String targetType, long slotId, int quantity, String key) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(targetType, slotId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("bookingReference").asText();
    }

    private long createFutureDarshanSlot(int capacity) {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        long darshanId = darshanRepository.insert(templeId, "Darshan " + UUID.randomUUID(), null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        return darshanSlotRepository.insert(darshanId, start, start.plusHours(1), capacity, DarshanSlotStatus.AVAILABLE).id();
    }

    private long createFutureRitualSlotWithPrice(BigDecimal price, int capacity) {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
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
}
