package com.temple.platform.ritual.api;

import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class RitualApiTest {

    private static final String PASSWORD = "ValidPass1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private TempleAdminAssignmentRepository assignmentRepository;

    @Autowired
    private RitualRepository ritualRepository;

    @Autowired
    private RitualSlotRepository slotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void capacityCannotBeReducedBelowConfirmedBookings() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Capacity Guard");
        Instant start = Instant.now().plus(Duration.ofDays(16));
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start.plus(Duration.ofHours(1)), 5)))
                .andExpect(status().isCreated())
                .andReturn();
        long slotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        long devoteeId = createAccount(AccountRole.DEVOTEE);
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                devoteeId,
                null,
                slotId,
                3,
                BookingStatus.CONFIRMED,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Slot capacity cannot be reduced below confirmed booking quantity"));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(5));
    }

    @Test
    void capacityCanBeReducedToConfirmedQuantity() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.HAVAN, "Exact Capacity");
        Instant start = Instant.now().plus(Duration.ofDays(17));
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start.plus(Duration.ofHours(1)), 5)))
                .andExpect(status().isCreated())
                .andReturn();
        long slotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        long devoteeId = createAccount(AccountRole.DEVOTEE);
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                devoteeId,
                null,
                slotId,
                3,
                BookingStatus.CONFIRMED,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(3));
    }

    @Test
    void capacityCanBeIncreasedWhenBookingsExist() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Increase Capacity");
        Instant start = Instant.now().plus(Duration.ofDays(18));
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start.plus(Duration.ofHours(1)), 5)))
                .andExpect(status().isCreated())
                .andReturn();
        long slotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        long devoteeId = createAccount(AccountRole.DEVOTEE);
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                devoteeId,
                null,
                slotId,
                2,
                BookingStatus.CONFIRMED,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(8));
    }

    @Test
    void platformAdminCanCreateGetListAndUpdateRituals() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);

        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "Ganesh Puja", 45, "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PUJA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andReturn();
        long ritualId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ganesh Puja"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\",\"name\":\"Evening Puja\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.name").value("Evening Puja"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void pujaAndHavanTypeFilter() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        createRitualViaApi(token, templeId, RitualType.PUJA, "Puja A");
        createRitualViaApi(token, templeId, RitualType.HAVAN, "Havan A");

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals?type=PUJA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("PUJA"));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals?type=HAVAN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("HAVAN"));
    }

    @Test
    void zeroPriceAllowedAndNegativePriceRejected() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "Free Puja", 20, "0")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(0));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.HAVAN, "Invalid", 20, "-1.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDurationNameTypeAndCurrencyRejected() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "Bad duration", 0, "10.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "   ", 30, "10.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"AARTI",
                                  "name":"Invalid type",
                                  "durationMinutes":30,
                                  "price":10.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"PUJA",
                                  "name":"Invalid currency",
                                  "durationMinutes":30,
                                  "price":10.00,
                                  "currency":"USD"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTempleReturns404() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        mockMvc.perform(post("/api/v1/temples/999999/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "Orphan", 30, "10.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignedTempleAdminCanManageUnassignedDenied() throws Exception {
        long templeId = createTemple();
        long assignedId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(assignedId, templeId);
        String assignedToken = loginAs(assignedId);
        String unassignedToken = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + assignedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.HAVAN, "Assigned Havan", 90, "500.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + unassignedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.HAVAN, "Denied", 90, "500.00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void devoteeCanReadButNotWrite() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long ritualId = createRitualViaApi(platformToken, templeId, RitualType.PUJA, "Public Puja");
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Public Puja"));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(RitualType.PUJA, "Denied", 30, "10.00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(Instant.now().plus(Duration.ofDays(2)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveTempleAndRitualHiddenFromDevotee() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long activeTempleId = createTempleViaApi(platformToken);
        long inactiveTempleId = templeRepository.insert(
                "Inactive " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "Asia/Kolkata",
                TempleStatus.INACTIVE
        ).id();
        long activeRitualId = createRitualViaApi(platformToken, activeTempleId, RitualType.PUJA, "Active");
        long inactiveRitualId = ritualRepository.insert(
                activeTempleId,
                RitualType.HAVAN,
                "Inactive ritual",
                null,
                30,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.INACTIVE
        ).id();
        long hiddenTempleRitualId = ritualRepository.insert(
                inactiveTempleId,
                RitualType.PUJA,
                "Hidden",
                null,
                30,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + activeTempleId + "/rituals/" + inactiveRitualId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/temples/" + inactiveTempleId + "/rituals/" + hiddenTempleRitualId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/temples/" + activeTempleId + "/rituals")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(activeRitualId));
    }

    @Test
    void nestedTempleRitualBolaReturns404() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeA = createTempleViaApi(token);
        long templeB = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeA, RitualType.PUJA, "Temple A");

        mockMvc.perform(get("/api/v1/temples/" + templeB + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void nestedRitualSlotBolaReturns404() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualA = createRitualViaApi(token, templeId, RitualType.PUJA, "Ritual A");
        long ritualB = createRitualViaApi(token, templeId, RitualType.HAVAN, "Ritual B");
        Instant start = Instant.now().plus(Duration.ofDays(4));
        var slot = slotRepository.insert(ritualA, start, start.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualB + "/slots/" + slot.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void slotLifecycleAndVisibility() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Slot ritual");
        Instant futureStart = Instant.now().plus(Duration.ofDays(12));
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(futureStart)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();
        long slotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String devoteeToken = registerDevotee();

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid slot status transition"));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void pastSlotHiddenFromDevotee() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Past");
        Instant pastStart = Instant.now().minus(Duration.ofDays(2));
        var past = slotRepository.insert(ritualId, pastStart, pastStart.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);
        Instant futureStart = Instant.now().plus(Duration.ofDays(3));
        slotRepository.insert(ritualId, futureStart, futureStart.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + past.id())
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidSlotScheduleRejected() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Schedule");
        Instant start = Instant.now().plus(Duration.ofDays(3));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slot end time must be after start time"));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt":"%s",
                                  "endAt":"%s",
                                  "capacity":0
                                }
                                """.formatted(start, start.plus(Duration.ofHours(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingRitualSlotsAreAllowed() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.HAVAN, "Parallel");
        Instant start = Instant.now().plus(Duration.ofDays(8));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start.plus(Duration.ofHours(2)))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start.plus(Duration.ofMinutes(30)), start.plus(Duration.ofHours(3)))))
                .andExpect(status().isCreated());
    }

    @Test
    void timezoneDateFilteringUsesTempleTimezone() throws Exception {
        long templeId = templeRepository.insert(
                "Timezone Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "Asia/Kolkata",
                TempleStatus.ACTIVE
        ).id();
        long ritualId = ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Morning",
                null,
                60,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        LocalDate targetDate = LocalDate.now(kolkata).plusDays(7);
        LocalDate previousDate = targetDate.minusDays(1);
        Instant start = targetDate.atStartOfDay(kolkata).toInstant();
        slotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?date=" + targetDate)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?date=" + previousDate)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void dstSafeLocalDateBoundaryForAmericaNewYork() throws Exception {
        long templeId = templeRepository.insert(
                "DST Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "America/New_York",
                TempleStatus.ACTIVE
        ).id();
        long ritualId = ritualRepository.insert(
                templeId,
                RitualType.HAVAN,
                "DST Havan",
                null,
                60,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        Instant start = Instant.parse("2027-03-14T05:00:00Z");
        slotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?date=2027-03-14")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?date=2027-03-13")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void dateAndFromToTogetherRejected() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Ambiguous");
        Instant from = Instant.now().plus(Duration.ofDays(1));
        Instant to = from.plus(Duration.ofDays(2));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId
                        + "/slots?date=2026-09-02&from=" + from + "&to=" + to)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot combine date with from or to query parameters"));
    }

    @Test
    void malformedDateRejected() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Date");

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?date=not-a-date")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginationLimitAndDeterministicOrdering() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        createRitualViaApi(token, templeId, RitualType.PUJA, "Alpha");
        createRitualViaApi(token, templeId, RitualType.PUJA, "Alpha");
        createRitualViaApi(token, templeId, RitualType.HAVAN, "Beta");

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals?size=101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        MvcResult listed = mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals?size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"))
                .andExpect(jsonPath("$.content[1].name").value("Alpha"))
                .andReturn();
        long firstId = objectMapper.readTree(listed.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();
        long secondId = objectMapper.readTree(listed.getResponse().getContentAsString())
                .get("content").get(1).get("id").asLong();
        assertThat(firstId).isLessThan(secondId);

        long ritualId = createRitualViaApi(token, templeId, RitualType.HAVAN, "Slots");
        Instant start = Instant.now().plus(Duration.ofDays(15));
        slotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), 10, RitualSlotStatus.AVAILABLE);
        slotRepository.insert(ritualId, start, start.plus(Duration.ofHours(2)), 10, RitualSlotStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?size=101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots?size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/temples/1/rituals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void durationChangeDoesNotRewriteSlots() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(token);
        long ritualId = createRitualViaApi(token, templeId, RitualType.PUJA, "Duration");
        Instant start = Instant.parse("2026-12-15T17:26:08.970450Z");
        Instant end = start.plus(Duration.ofMinutes(45));
        var slot = slotRepository.insert(ritualId, start, end, 10, RitualSlotStatus.AVAILABLE);

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/rituals/" + ritualId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(90));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/rituals/" + ritualId + "/slots/" + slot.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").exists())
                .andExpect(jsonPath("$.endAt").exists());
        assertThat(slotRepository.findByRitualIdAndId(ritualId, slot.id()).orElseThrow().endAt())
                .isEqualTo(end);
    }

    private long createTemple() {
        return templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                "Description",
                "City",
                "State",
                "Country",
                "Asia/Kolkata",
                TempleStatus.ACTIVE
        ).id();
    }

    private long createTempleViaApi(String platformToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/temples")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Temple %s",
                                  "description":"A temple",
                                  "city":"City",
                                  "state":"State",
                                  "country":"Country",
                                  "timezone":"Asia/Kolkata",
                                  "status":"ACTIVE"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createRitualViaApi(String token, long templeId, RitualType type, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/temples/" + templeId + "/rituals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ritualJson(type, name, 30, "10.00")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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

    private static String ritualJson(RitualType type, String name, int durationMinutes, String price) {
        return """
                {
                  "type":"%s",
                  "name":"%s",
                  "description":"Ritual description",
                  "durationMinutes":%d,
                  "price":%s
                }
                """.formatted(type.name(), name, durationMinutes, price);
    }

    private static String slotJson(Instant start) {
        return slotJson(start, start.plus(Duration.ofHours(1)));
    }

    private static String slotJson(Instant start, Instant end) {
        return slotJson(start, end, 10);
    }

    private static String slotJson(Instant start, Instant end, int capacity) {
        return """
                {
                  "startAt":"%s",
                  "endAt":"%s",
                  "capacity":%d
                }
                """.formatted(start, end, capacity);
    }
}
