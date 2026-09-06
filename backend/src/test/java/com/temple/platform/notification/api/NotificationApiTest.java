package com.temple.platform.notification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.Notification;
import com.temple.platform.notification.domain.NotificationChannel;
import com.temple.platform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class NotificationApiTest {

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
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanNotifications() {
        jdbcTemplate.update("DELETE FROM notification");
    }

    @Test
    void authenticatedOwnerCanListAndReadOwnNotifications() throws Exception {
        String token = registerDevotee();
        long accountId = accountIdForToken(token);
        Notification notification = seedNotification(accountId, DomainEventType.BOOKING_CONFIRMED);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].notificationReference")
                        .value(notification.notificationReference().toString()))
                .andExpect(jsonPath("$.content[0].status").value("SENT"));

        mockMvc.perform(get("/api/v1/notifications/" + notification.notificationReference())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(notification.title()))
                .andExpect(jsonPath("$.type").value("BOOKING_CONFIRMED"));
    }

    @Test
    void crossAccountDetailReturns404() throws Exception {
        String owner = registerDevotee();
        String other = registerDevotee();
        Notification notification = seedNotification(accountIdForToken(owner), DomainEventType.PAYMENT_SUCCEEDED);

        mockMvc.perform(get("/api/v1/notifications/" + notification.notificationReference())
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        Notification notification = seedNotification(createAccount(AccountRole.DEVOTEE), DomainEventType.BOOKING_CANCELLED);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/notifications/" + notification.notificationReference()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void platformAdminCanListAllAndReadAnyNotification() throws Exception {
        long ownerA = createAccount(AccountRole.DEVOTEE);
        long ownerB = createAccount(AccountRole.DEVOTEE);
        seedNotification(ownerA, DomainEventType.BOOKING_CONFIRMED);
        Notification second = seedNotification(ownerB, DomainEventType.PAYMENT_FAILED);
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/notifications/" + second.notificationReference())
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationReference")
                        .value(second.notificationReference().toString()))
                .andExpect(jsonPath("$.type").value("PAYMENT_FAILED"));
    }

    private Notification seedNotification(long accountId, DomainEventType type) {
        Notification pending = notificationRepository.insertIfAbsent(
                UUID.randomUUID(),
                accountId,
                NotificationChannel.EMAIL_MOCK,
                type,
                "Title for " + type.name(),
                "Message for " + type.name(),
                UUID.randomUUID()
        ).orElseThrow();
        notificationRepository.markSent(pending.id(), java.time.Instant.parse("2026-01-01T00:00:00Z"));
        return notificationRepository.findByNotificationReference(pending.notificationReference()).orElseThrow();
    }

    private long accountIdForToken(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String email = objectMapper.readTree(me.getResponse().getContentAsString()).get("email").asText();
        return accountRepository.findByEmail(email).orElseThrow().id();
    }

    private long createAccount(AccountRole role) {
        return accountRepository.insert(
                role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                role,
                AccountStatus.ACTIVE
        ).id();
    }

    private String loginAs(long accountId) throws Exception {
        return login(accountRepository.findById(accountId).orElseThrow().email());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class).accessToken();
    }

    private String registerDevotee() throws Exception {
        String email = "devotee-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email);
    }
}
