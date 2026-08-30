package com.temple.platform.temple.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.platform.identity.api.dto.LoginResponse;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.temple.domain.EventStatus;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleEventRepository;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TempleApiTest {

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
    private TempleEventRepository eventRepository;

    @Test
    void missingJwtOnTempleCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/temples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTempleJson(TempleStatus.ACTIVE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devoteeCannotCreateTemple() throws Exception {
        String token = registerDevotee();
        mockMvc.perform(post("/api/v1/temples")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTempleJson(TempleStatus.ACTIVE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void templeAdminCannotCreateTemple() throws Exception {
        String token = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));
        mockMvc.perform(post("/api/v1/temples")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTempleJson(TempleStatus.ACTIVE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCanCreateTemple() throws Exception {
        String token = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        mockMvc.perform(post("/api/v1/temples")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTempleJson(TempleStatus.ACTIVE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void platformAdminCanAssignTempleAdmin() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + templeAdminId + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void nonTempleAdminAccountCannotBeAssigned() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long devoteeId = createAccount(AccountRole.DEVOTEE);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + devoteeId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateAssignmentReturns409() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        String body = "{\"accountId\":" + templeAdminId + "}";

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Temple admin assignment already exists"));
    }

    @Test
    void assignedTempleAdminCanUpdateTemple() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(templeAdminId, templeId);
        String templeAdminToken = loginAs(templeAdminId);

        mockMvc.perform(patch("/api/v1/temples/" + templeId)
                        .header("Authorization", "Bearer " + templeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Temple Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Temple Name"));
    }

    @Test
    void unassignedTempleAdminDenied() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        String token = loginAs(createAccount(AccountRole.TEMPLE_ADMIN));

        mockMvc.perform(patch("/api/v1/temples/" + templeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Denied Update\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedTempleAdminCanCreateAndUpdateEvent() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(templeAdminId, templeId);
        String templeAdminToken = loginAs(templeAdminId);

        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + templeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Festival")))
                .andExpect(status().isCreated())
                .andReturn();
        long eventId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + eventId)
                        .header("Authorization", "Bearer " + templeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void crossTempleEventAccessCannotSucceed() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeA = createTempleViaApi(platformToken);
        long templeB = createTempleViaApi(platformToken);
        var event = eventRepository.insert(
                templeA,
                "Temple A Event",
                null,
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                EventStatus.PUBLISHED
        );

        mockMvc.perform(get("/api/v1/temples/" + templeB + "/events/" + event.id())
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidEventScheduleReturns400() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Bad Event",
                                  "startAt":"%s",
                                  "endAt":"%s"
                                }
                                """.formatted(start, start)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Event end time must be after start time"));
    }

    @Test
    void publicReadDoesNotExposeDraftEvents() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        eventRepository.insert(
                templeId,
                "Draft Event",
                null,
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                EventStatus.DRAFT
        );
        eventRepository.insert(
                templeId,
                "Published Event",
                null,
                OffsetDateTime.now().plusDays(4),
                OffsetDateTime.now().plusDays(5),
                EventStatus.PUBLISHED
        );
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    void publicReadDoesNotExposeInactiveTemples() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long inactiveTempleId = templeRepository.insert(
                "Inactive Temple " + UUID.randomUUID(),
                "Hidden",
                "City",
                "State",
                "Country",
                "Asia/Kolkata",
                TempleStatus.INACTIVE
        ).id();
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + inactiveTempleId)
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());

        MvcResult listResult = mockMvc.perform(get("/api/v1/temples")
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode temples = objectMapper.readTree(listResult.getResponse().getContentAsString());
        for (JsonNode temple : temples) {
            assertThat(temple.get("id").asLong()).isNotEqualTo(inactiveTempleId);
        }
    }

    @Test
    void paginationMaximumIsEnforced() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events?size=101")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devoteeCannotUpdateTempleOrEvents() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        var event = eventRepository.insert(
                templeId,
                "Event",
                null,
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                EventStatus.PUBLISHED
        );
        String devoteeToken = registerDevotee();

        mockMvc.perform(patch("/api/v1/temples/" + templeId)
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + event.id())
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignmentRemovalReturns204() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(templeAdminId, templeId);

        mockMvc.perform(delete("/api/v1/temples/" + templeId + "/admins/" + templeAdminId)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void devoteeGetDraftEventReturns404() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        var draftEvent = eventRepository.insert(
                templeId,
                "Draft Event",
                null,
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                EventStatus.DRAFT
        );
        String devoteeToken = registerDevotee();

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events/" + draftEvent.id())
                        .header("Authorization", "Bearer " + devoteeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void devoteeCannotPostEvent() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        String devoteeToken = registerDevotee();

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + devoteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Denied Event")))
                .andExpect(status().isForbidden());
    }

    @Test
    void templeAdminCannotAssignOrRemoveAdmins() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long templeAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        long otherTempleAdminId = createAccount(AccountRole.TEMPLE_ADMIN);
        assignmentRepository.insert(templeAdminId, templeId);
        String templeAdminToken = loginAs(templeAdminId);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + templeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + otherTempleAdminId + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/temples/" + templeId + "/admins/" + otherTempleAdminId)
                        .header("Authorization", "Bearer " + templeAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCanManageUnassignedTemple() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(patch("/api/v1/temples/" + templeId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform Managed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Platform Managed"));

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Platform Event")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void disabledTempleAdminCannotBeAssigned() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long disabledAdminId = accountRepository.insert(
                "disabled-admin-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                AccountRole.TEMPLE_ADMIN,
                AccountStatus.DISABLED
        ).id();

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/admins")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + disabledAdminId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventCreationDefaultsToDraft() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("New Event")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void invalidLifecycleTransitionsReturn400() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long publishedEventId = createEventViaApi(platformToken, templeId, EventStatus.PUBLISHED);
        long cancelledEventId = createEventViaApi(platformToken, templeId, EventStatus.CANCELLED);

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + publishedEventId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid event status transition"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + cancelledEventId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid event status transition"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + cancelledEventId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid event status transition"));
    }

    @Test
    void validLifecycleTransitionsSucceed() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);
        long draftEventId = createEventViaApi(platformToken, templeId, EventStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + draftEventId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        long publishableDraftId = createEventViaApi(platformToken, templeId, EventStatus.DRAFT);
        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + publishableDraftId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + publishableDraftId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        long fieldUpdateDraftId = createEventViaApi(platformToken, templeId, EventStatus.DRAFT);
        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + fieldUpdateDraftId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Draft Event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Draft Event"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void paginationOverflowReturns400() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events?page=30000000&size=100")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginationBoundaryBehavior() throws Exception {
        String platformToken = loginAs(createAccount(AccountRole.PLATFORM_ADMIN));
        long templeId = createTempleViaApi(platformToken);

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events?size=100")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(get("/api/v1/temples/" + templeId + "/events?page=-1")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    private long createEventViaApi(String platformToken, long templeId, EventStatus status) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/temples/" + templeId + "/events")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Event " + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        long eventId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        if (status == EventStatus.DRAFT) {
            return eventId;
        }
        mockMvc.perform(patch("/api/v1/temples/" + templeId + "/events/" + eventId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status.name() + "\"}"))
                .andExpect(status().isOk());
        return eventId;
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

    private long createTempleViaApi(String platformToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/temples")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTempleJson(TempleStatus.ACTIVE)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String createTempleJson(TempleStatus status) {
        return """
                {
                  "name":"Temple %s",
                  "description":"A temple",
                  "city":"City",
                  "state":"State",
                  "country":"Country",
                  "timezone":"Asia/Kolkata",
                  "status":"%s"
                }
                """.formatted(UUID.randomUUID(), status.name());
    }

    private static String eventJson(String name) {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.plusHours(2);
        return """
                {
                  "name":"%s",
                  "description":"Event",
                  "startAt":"%s",
                  "endAt":"%s"
                }
                """.formatted(name, start, end);
    }
}
