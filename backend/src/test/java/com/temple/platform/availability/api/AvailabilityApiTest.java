package com.temple.platform.availability.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
class AvailabilityApiTest {

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
    void darshanSlotWithNoBookingsShowsFullAvailability() throws Exception {
        DarshanFixture fixture = createDarshanFixture(10);
        String token = registerDevotee();

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(fixture.slotId()))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.bookedQuantity").value(0))
                .andExpect(jsonPath("$.remainingCapacity").value(10))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void confirmedBookingReducesDarshanAvailability() throws Exception {
        DarshanFixture fixture = createDarshanFixture(10);
        String token = registerDevotee();
        createBooking(token, "DARSHAN", fixture.slotId(), 4);

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedQuantity").value(4))
                .andExpect(jsonPath("$.remainingCapacity").value(6))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void multipleConfirmedBookingsAggregateForDarshanList() throws Exception {
        DarshanFixture fixture = createDarshanFixture(10);
        String first = registerDevotee();
        String second = registerDevotee();
        createBooking(first, "DARSHAN", fixture.slotId(), 2);
        createBooking(second, "DARSHAN", fixture.slotId(), 3);

        MvcResult result = mockMvc.perform(get("/api/v1/temples/%d/darshans/%d/availability"
                                .formatted(fixture.templeId(), fixture.darshanId()))
                        .header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn();

        JsonNode slot = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("content").get(0);
        assertThat(slot.get("bookedQuantity").asInt()).isEqualTo(5);
        assertThat(slot.get("remainingCapacity").asInt()).isEqualTo(5);
    }

    @Test
    void cancelledBookingDoesNotConsumeDarshanCapacity() throws Exception {
        DarshanFixture fixture = createDarshanFixture(5);
        String token = registerDevotee();
        String reference = createBooking(token, "DARSHAN", fixture.slotId(), 3);

        mockMvc.perform(patch("/api/v1/bookings/" + reference)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedQuantity").value(0))
                .andExpect(jsonPath("$.remainingCapacity").value(5))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void fullDarshanSlotShowsZeroRemainingAndUnavailable() throws Exception {
        DarshanFixture fixture = createDarshanFixture(2);
        String first = registerDevotee();
        String second = registerDevotee();
        createBooking(first, "DARSHAN", fixture.slotId(), 1);
        createBooking(second, "DARSHAN", fixture.slotId(), 1);

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedQuantity").value(2))
                .andExpect(jsonPath("$.remainingCapacity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void overCapacityDataNeverExposesNegativeRemainingCapacity() throws Exception {
        DarshanFixture fixture = createDarshanFixture(2);
        long accountId = createAccount(AccountRole.DEVOTEE);
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, fixture.slotId(), null, 3, BookingStatus.CONFIRMED, "over1");
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, fixture.slotId(), null, 2, BookingStatus.CONFIRMED, "over2");
        String token = loginAs(accountId);

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedQuantity").value(5))
                .andExpect(jsonPath("$.remainingCapacity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void ritualAvailabilityWorks() throws Exception {
        RitualFixture fixture = createRitualFixture(8);
        String token = registerDevotee();
        createBooking(token, "RITUAL", fixture.slotId(), 3);

        mockMvc.perform(get(ritualAvailabilityPath(fixture.templeId(), fixture.ritualId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(fixture.slotId()))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.bookedQuantity").value(3))
                .andExpect(jsonPath("$.remainingCapacity").value(5))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void ritualListAvailabilityAggregatesMultipleSlotsInOneResponse() throws Exception {
        RitualFixture fixture = createRitualFixture(4);
        Instant start = Instant.now().plus(Duration.ofDays(7));
        long secondSlotId = ritualSlotRepository.insert(
                fixture.ritualId(),
                start.plus(Duration.ofHours(2)),
                start.plus(Duration.ofHours(3)),
                6,
                RitualSlotStatus.AVAILABLE
        ).id();
        String first = registerDevotee();
        String second = registerDevotee();
        createBooking(first, "RITUAL", fixture.slotId(), 1);
        createBooking(second, "RITUAL", secondSlotId, 2);

        MvcResult result = mockMvc.perform(get("/api/v1/temples/%d/rituals/%d/availability"
                                .formatted(fixture.templeId(), fixture.ritualId()))
                        .header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        assertThat(content).hasSize(2);
        assertThat(content.findValues("bookedQuantity")).extracting(JsonNode::asInt).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void authorizationRulesArePreservedForAvailability() throws Exception {
        DarshanFixture fixture = createDarshanFixture(5);
        String devotee = registerDevotee();
        String other = registerDevotee();

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId())))
                .andExpect(status().isUnauthorized());

        long assignedAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(assignedAdminId, fixture.templeId());
        String assignedAdmin = loginAs(assignedAdminId);
        String unassignedAdmin = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + assignedAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + unassignedAdmin))
                .andExpect(status().isNotFound());

        long inactiveTempleId = templeRepository.insert(
                "Hidden " + UUID.randomUUID(), null, "City", "State", "Country", "UTC", TempleStatus.INACTIVE).id();
        long hiddenDarshanId = darshanRepository.insert(
                inactiveTempleId, "Hidden", null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        long hiddenSlotId = darshanSlotRepository.insert(
                hiddenDarshanId, start, start.plusHours(1), 5, DarshanSlotStatus.AVAILABLE).id();

        mockMvc.perform(get(darshanAvailabilityPath(inactiveTempleId, hiddenDarshanId, hiddenSlotId))
                        .header("Authorization", "Bearer " + devotee))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), 999_999L, fixture.slotId()))
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), 999_999L))
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingCreateStillUsesLockingAfterAvailabilityReads() throws Exception {
        DarshanFixture fixture = createDarshanFixture(1);
        String first = registerDevotee();
        String second = registerDevotee();
        String token = first;

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCapacity").value(1));

        createBooking(first, "DARSHAN", fixture.slotId(), 1);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + second)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("DARSHAN", fixture.slotId(), 1)))
                .andExpect(status().isConflict());

        mockMvc.perform(get(darshanAvailabilityPath(fixture.templeId(), fixture.darshanId(), fixture.slotId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCapacity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    private record DarshanFixture(long templeId, long darshanId, long slotId) {
    }

    private record RitualFixture(long templeId, long ritualId, long slotId) {
    }

    private DarshanFixture createDarshanFixture(int capacity) {
        long templeId = createTemple();
        long darshanId = darshanRepository.insert(
                templeId, "Darshan " + UUID.randomUUID(), null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        long slotId = darshanSlotRepository.insert(
                darshanId, start, start.plusHours(1), capacity, DarshanSlotStatus.AVAILABLE).id();
        return new DarshanFixture(templeId, darshanId, slotId);
    }

    private RitualFixture createRitualFixture(int capacity) {
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
        long slotId = ritualSlotRepository.insert(
                ritualId, start, start.plus(Duration.ofHours(1)), capacity, RitualSlotStatus.AVAILABLE).id();
        return new RitualFixture(templeId, ritualId, slotId);
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

    private static String darshanAvailabilityPath(long templeId, long darshanId, long slotId) {
        return "/api/v1/temples/%d/darshans/%d/slots/%d/availability".formatted(templeId, darshanId, slotId);
    }

    private static String ritualAvailabilityPath(long templeId, long ritualId, long slotId) {
        return "/api/v1/temples/%d/rituals/%d/slots/%d/availability".formatted(templeId, ritualId, slotId);
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
