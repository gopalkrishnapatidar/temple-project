package com.temple.platform.donation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.donation.repository.DonationRepository;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.repository.PaymentRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DonationApiTest {

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
    private DonationRepository donationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void validDonationCreatesPendingPayment() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "100.50", "INR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.paymentReference").isNotEmpty());
    }

    @Test
    void zeroOrNegativeDonationRejected() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "0.00", "INR")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "-5.00", "INR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excessivePrecisionRejected() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "10.999", "INR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCurrencyRejected() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "10.00", "USD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void donationBolaReturns404() throws Exception {
        long templeId = createTemple();
        String owner = registerDevotee();
        String other = registerDevotee();
        MvcResult created = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + owner)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "25.00", "INR")))
                .andExpect(status().isCreated())
                .andReturn();
        String donationReference = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("donationReference").asText();

        mockMvc.perform(get("/api/v1/donations/" + donationReference)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulDonationPaymentUpdatesBusinessStatus() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "10.00", "INR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();
        String donationReference = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("donationReference").asText();
        var donation = donationRepository.findByDonationReference(UUID.fromString(donationReference)).orElseThrow();
        assertThat(donation.status()).isEqualTo(DonationStatus.COMPLETED);
        var payment = paymentRepository.findByAccountIdAndIdempotencyKey(
                donation.accountId(),
                "donation-pay-" + key
        ).orElseThrow();
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.donationId()).isEqualTo(donation.id());
    }

    @Test
    void getDonationReturnsLinkedPaymentReference() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();
        MvcResult created = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "25.00", "INR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andReturn();
        String donationReference = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("donationReference").asText();
        String paymentReference = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("paymentReference").asText();

        mockMvc.perform(get("/api/v1/donations/" + donationReference)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value(paymentReference));
    }

    @Test
    void sameIdempotencyKeyRetriesDonation() throws Exception {
        long templeId = createTemple();
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();
        MvcResult first = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "15.00", "INR")))
                .andExpect(status().isCreated())
                .andReturn();
        String firstReference = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("donationReference").asText();

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson(templeId, "15.00", "INR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donationReference").value(firstReference));
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

    private static String donationJson(long templeId, String amount, String currency) {
        return """
                {
                  "templeId":%d,
                  "amount":%s,
                  "currency":"%s"
                }
                """.formatted(templeId, amount, currency);
    }
}
