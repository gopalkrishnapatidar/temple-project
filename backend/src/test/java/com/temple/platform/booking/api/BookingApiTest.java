package com.temple.platform.booking.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BookingApiTest {

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
    private DarshanSlotRepository darshanSlotRepository;

    @Autowired
    private RitualRepository ritualRepository;

    @Autowired
    private RitualSlotRepository ritualSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void devoteeCanBookDarshanSlot() throws Exception {
        long slotId = createFutureDarshanSlot(10);
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("DARSHAN"))
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());
    }

    @Test
    void devoteeCanBookRitualSlot() throws Exception {
        long slotId = createFutureRitualSlot(6);
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("RITUAL", slotId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("RITUAL"))
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void quantityValidationRejected() throws Exception {
        long slotId = createFutureDarshanSlot(10);
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 0)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 51)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingIdempotencyKeyRejected() throws Exception {
        long slotId = createFutureDarshanSlot(10);
        String token = registerDevotee();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelledPastAndInactiveSlotsRejected() throws Exception {
        String token = registerDevotee();
        long cancelledId = createDarshanSlot(10, DarshanSlotStatus.CANCELLED, OffsetDateTime.now().plusDays(3));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", cancelledId, 1)))
                .andExpect(status().isConflict());

        long pastId = createDarshanSlot(10, DarshanSlotStatus.AVAILABLE, OffsetDateTime.now().minusDays(2));
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", pastId, 1)))
                .andExpect(status().isConflict());

        long inactiveTempleId = templeRepository.insert(
                "Inactive " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.INACTIVE
        ).id();
        long darshanId = darshanRepository.insert(inactiveTempleId, "Hidden", null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(4);
        long hiddenSlotId = darshanSlotRepository.insert(
                darshanId, start, start.plusHours(1), 10, DarshanSlotStatus.AVAILABLE).id();
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", hiddenSlotId, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void capacityIsEnforcedAndCancellationReleasesIt() throws Exception {
        long slotId = createFutureDarshanSlot(2);
        String first = registerDevotee();
        String second = registerDevotee();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + first)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + second)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isConflict());

        MvcResult listed = mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();
        String reference = objectMapper.readTree(listed.getResponse().getContentAsString())
                .get("content").get(0).get("bookingReference").asText();

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + second)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isCreated());
        assertThat(bookingRepository.sumConfirmedQuantityForDarshanSlot(slotId)).isEqualTo(1);
    }

    @Test
    void devoteeCannotReadOrCancelAnotherBooking() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String owner = registerDevotee();
        String other = registerDevotee();
        String reference = createBooking(owner, "DARSHAN", slotId, 1);

        mockMvc.perform(get("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void platformAdminCanReadAndCancelAnyBooking() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String devotee = registerDevotee();
        String reference = createBooking(devotee, "DARSHAN", slotId, 1);
        String platform = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));

        mockMvc.perform(get("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + platform))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value(reference));

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + platform)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void assignedTempleAdminCanManageUnassignedCannot() throws Exception {
        long templeId = createTemple();
        long slotId = createFutureDarshanSlotAtTemple(templeId, 5);
        String devotee = registerDevotee();
        String reference = createBooking(devotee, "DARSHAN", slotId, 1);

        long assignedId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(assignedId, templeId);
        String assigned = loginAs(assignedId);
        String unassigned = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));

        mockMvc.perform(get("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + assigned))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + unassigned))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + unassigned)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + assigned)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminsCannotCreateBookings() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String platform = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + platform)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void idempotentReplayReturnsExistingBooking() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isCreated())
                .andReturn();
        String reference = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("bookingReference").asText();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(reference));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", slotId, 2)))
                .andExpect(status().isConflict());

        assertThat(bookingRepository.sumConfirmedQuantityForDarshanSlot(slotId)).isEqualTo(1);
    }

    @Test
    void listingIsOwnerScopedAndDeterministic() throws Exception {
        long slotId = createFutureDarshanSlot(10);
        String owner = registerDevotee();
        String other = registerDevotee();
        String firstRef = createBooking(owner, "DARSHAN", slotId, 1);
        String secondRef = createBooking(owner, "DARSHAN", slotId, 1);

        mockMvc.perform(get("/api/v1/bookings?size=101")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());

        MvcResult listed = mockMvc.perform(get("/api/v1/bookings?size=1")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn();
        String newest = objectMapper.readTree(listed.getResponse().getContentAsString())
                .get("content").get(0).get("bookingReference").asText();
        assertThat(newest).isEqualTo(secondRef);

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/bookings/" + firstRef)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value(firstRef));
    }

    @Test
    void unauthenticatedBookingRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBookingsDoNotOversell() throws Exception {
        long slotId = createFutureDarshanSlot(1);
        String first = registerDevotee();
        String second = registerDevotee();
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            Future<?> one = executor.submit(() -> bookConcurrently(first, slotId, created, conflicted, ready, go));
            Future<?> two = executor.submit(() -> bookConcurrently(second, slotId, created, conflicted, ready, go));
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            one.get(15, TimeUnit.SECONDS);
            two.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(1);
        assertThat(conflicted.get()).isEqualTo(1);
        assertThat(bookingRepository.sumConfirmedQuantityForDarshanSlot(slotId)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameIdempotencyKeyCreatesOneBooking() throws Exception {
        long slotId = createFutureDarshanSlot(5);
        String token = registerDevotee();
        String key = UUID.randomUUID().toString();
        AtomicInteger created = new AtomicInteger();
        AtomicReference<String> firstReference = new AtomicReference<>();
        AtomicReference<String> secondReference = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            Future<?> one = executor.submit(() -> {
                ready.countDown();
                await(go);
                String reference = postBooking(token, key, slotId);
                created.incrementAndGet();
                firstReference.compareAndSet(null, reference);
                secondReference.set(reference);
            });
            Future<?> two = executor.submit(() -> {
                ready.countDown();
                await(go);
                String reference = postBooking(token, key, slotId);
                created.incrementAndGet();
                firstReference.compareAndSet(null, reference);
                secondReference.set(reference);
            });
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            one.get(15, TimeUnit.SECONDS);
            two.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(2);
        assertThat(firstReference.get()).isEqualTo(secondReference.get());
        assertThat(bookingRepository.sumConfirmedQuantityForDarshanSlot(slotId)).isEqualTo(1);
    }

    private void bookConcurrently(
            String token,
            long slotId,
            AtomicInteger created,
            AtomicInteger conflicted,
            CountDownLatch ready,
            CountDownLatch go) {
        ready.countDown();
        await(go);
        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson("DARSHAN", slotId, 1)))
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

    private String postBooking(String token, String key, long slotId) {
        try {
            MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson("DARSHAN", slotId, 1)))
                    .andExpect(status().isCreated())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("bookingReference").asText();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
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

    private long createFutureDarshanSlot(int capacity) {
        return createFutureDarshanSlotAtTemple(createTemple(), capacity);
    }

    private long createFutureDarshanSlotAtTemple(long templeId, int capacity) {
        return createDarshanSlotAtTemple(templeId, capacity, DarshanSlotStatus.AVAILABLE, OffsetDateTime.now().plusDays(5));
    }

    private long createDarshanSlot(int capacity, DarshanSlotStatus status, OffsetDateTime start) {
        return createDarshanSlotAtTemple(createTemple(), capacity, status, start);
    }

    private long createDarshanSlotAtTemple(
            long templeId,
            int capacity,
            DarshanSlotStatus status,
            OffsetDateTime start) {
        long darshanId = darshanRepository.insert(templeId, "Darshan " + UUID.randomUUID(), null, DarshanStatus.ACTIVE).id();
        return darshanSlotRepository.insert(darshanId, start, start.plusHours(1), capacity, status).id();
    }

    private long createFutureRitualSlot(int capacity) {
        long templeId = createTemple();
        long ritualId = ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Puja " + UUID.randomUUID(),
                null,
                30,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        Instant start = Instant.now().plus(Duration.ofDays(6));
        return ritualSlotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), capacity, RitualSlotStatus.AVAILABLE).id();
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

    private static String bookingJson(String targetType, long slotId, int quantity) {
        return """
                {
                  "targetType":"%s",
                  "slotId":%d,
                  "quantity":%d
                }
                """.formatted(targetType, slotId, quantity);
    }
}
