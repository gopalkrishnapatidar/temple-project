package com.temple.platform.darshan.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
class DarshanApiTest {

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
    private TempleAdminAssignmentRepository assignmentRepository;

    @Autowired
    private DarshanRepository darshanRepository;

    @Autowired
    private DarshanSlotRepository slotRepository;

    @Test
    void devoteeCannotCreateDarshan() throws Exception {
        long templeId = createTemple();
        String devoteeToken = registerDevotee();

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans")
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(darshanJson("Morning Darshan")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedTempleAdminCanCreateDarshanAndSlot() throws Exception {
        long templeId = createTemple();
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(templeAdminId, templeId);
        String token = loginAs(templeAdminId);

        long darshanId = createDarshanViaApi(token, templeId, "Morning Darshan");

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(OffsetDateTime.now().plusDays(3), 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void unassignedTempleAdminDenied() throws Exception {
        long templeId = createTemple();
        String token = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(darshanJson("Denied")))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCanManageDarshanAndSlots() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Special Darshan");

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(OffsetDateTime.now().plusDays(4), 80)))
                .andExpect(status().isCreated());
    }

    @Test
    void crossTempleDarshanAccessReturns404() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeA = createTempleViaApi(platformToken);
        long templeB = createTempleViaApi(platformToken);
        long darshanId = darshanRepository.insert(templeA, "Temple A Darshan", null, DarshanStatus.ACTIVE).id();

        mockMvc.perform(get("/api/v1/temples/" + templeB + "/darshans/" + darshanId)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossDarshanSlotAccessReturns404() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanA = createDarshanViaApi(platformToken, templeId, "Darshan A");
        long darshanB = createDarshanViaApi(platformToken, templeId, "Darshan B");
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        var slot = slotRepository.insert(darshanA, start, start.plusHours(1), 50, DarshanSlotStatus.AVAILABLE);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + darshanB + "/slots/" + slot.id())
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidSlotScheduleReturns400() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Darshan");
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, start, 50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slot end time must be after start time"));
    }

    @Test
    void invalidCapacityReturns400() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Darshan");
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt":"%s",
                                  "endAt":"%s",
                                  "capacity":0
                                }
                                """.formatted(start, start.plusHours(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingSlotsReturn409() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Darshan");
        OffsetDateTime start = OffsetDateTime.now().plusDays(8);
        OffsetDateTime middle = start.plusHours(1);
        OffsetDateTime end = start.plusHours(2);
        String first = slotJson(start, middle, 50);
        String overlapping = slotJson(middle.minusMinutes(30), end, 50);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlapping))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Darshan slot time range overlaps an existing available slot"));
    }

    @Test
    void adjacentSlotsAllowed() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Darshan");
        OffsetDateTime start = OffsetDateTime.now().plusDays(9);
        OffsetDateTime middle = start.plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, middle, 50)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(middle, end, 50)))
                .andExpect(status().isCreated());
    }

    @Test
    void devoteeSeesOnlyActiveDarshanAndValidSlots() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long activeDarshanId = createDarshanViaApi(platformToken, templeId, "Active");
        long inactiveDarshanId = darshanRepository.insert(templeId, "Inactive", null, DarshanStatus.INACTIVE).id();
        OffsetDateTime futureStart = OffsetDateTime.now().plusDays(10);
        slotRepository.insert(activeDarshanId, futureStart, futureStart.plusHours(1), 50, DarshanSlotStatus.AVAILABLE);
        OffsetDateTime pastStart = OffsetDateTime.now().minusDays(2);
        slotRepository.insert(activeDarshanId, pastStart, pastStart.plusHours(1), 50, DarshanSlotStatus.AVAILABLE);
        slotRepository.insert(activeDarshanId, futureStart.plusHours(2), futureStart.plusHours(3), 50, DarshanSlotStatus.CANCELLED);
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + inactiveDarshanId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + activeDarshanId + "/slots")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void cancellationHidesSlotFromDevoteeButNotAdmin() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Darshan");
        OffsetDateTime start = OffsetDateTime.now().plusDays(11);
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(start, 50)))
                .andExpect(status().isCreated())
                .andReturn();
        long slotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String devoteeToken = registerDevotee();

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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
        long darshanId = darshanRepository.insert(templeId, "Morning", null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.of(2026, 9, 1, 18, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);
        slotRepository.insert(darshanId, start, end, 50, DarshanSlotStatus.AVAILABLE);
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots?date=2026-09-02")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots?date=2026-09-01")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void darshanCreationDefaultsToActive() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(darshanJson("Default Active")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOverlappingCreationRejected() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long darshanId = createDarshanViaApi(platformToken, templeId, "Concurrent");
        OffsetDateTime start = OffsetDateTime.now().plusDays(20);
        OffsetDateTime end = start.plusHours(2);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> {
                ready.countDown();
                await(go);
                try {
                    mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                                    .header("Authorization", "Bearer " + platformToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(slotJson(start, start.plusHours(1), 50)))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 201) {
                                    created.incrementAndGet();
                                } else if (status == 409) {
                                    rejected.incrementAndGet();
                                } else {
                                    throw new AssertionError("Unexpected status " + status);
                                }
                            });
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            Future<?> second = executor.submit(() -> {
                ready.countDown();
                await(go);
                try {
                    mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans/" + darshanId + "/slots")
                                    .header("Authorization", "Bearer " + platformToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(slotJson(start.plusMinutes(30), end, 50)))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 201) {
                                    created.incrementAndGet();
                                } else if (status == 409) {
                                    rejected.incrementAndGet();
                                } else {
                                    throw new AssertionError("Unexpected status " + status);
                                }
                            });
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
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

    private long createDarshanViaApi(String token, long templeId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/temples/" + templeId + "/darshans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(darshanJson(name)))
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
        String email = accountRepository.findById(accountId).orElseThrow().email();
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

    private String registerDevotee() throws Exception {
        String email = "devotee-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private static String darshanJson(String name) {
        return """
                {
                  "name":"%s",
                  "description":"Darshan description"
                }
                """.formatted(name);
    }

    private static String slotJson(OffsetDateTime start, int capacity) {
        return slotJson(start, start.plusHours(1), capacity);
    }

    private static String slotJson(OffsetDateTime start, OffsetDateTime end, int capacity) {
        return """
                {
                  "startAt":"%s",
                  "endAt":"%s",
                  "capacity":%d
                }
                """.formatted(start, end, capacity);
    }
}
