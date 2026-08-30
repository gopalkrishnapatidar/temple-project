package com.temple.platform.temple.repository;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import com.temple.platform.identity.repository.AccountRepository;
import com.temple.platform.temple.domain.EventStatus;
import com.temple.platform.temple.domain.TempleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class TempleRepositoryTest {

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private TempleAdminAssignmentRepository assignmentRepository;

    @Autowired
    private TempleEventRepository eventRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void duplicateAssignmentIsRejected() {
        long accountId = createTempleAdminAccount();
        long templeId = createTemple().id();
        assignmentRepository.insert(accountId, templeId);

        assertThatThrownBy(() -> assignmentRepository.insert(accountId, templeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void eventEndMustBeAfterStart() {
        long templeId = createTemple().id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        assertThatThrownBy(() -> eventRepository.insert(
                templeId,
                "Invalid Event",
                null,
                start,
                start,
                EventStatus.DRAFT
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void eventBelongsToTemple() {
        long templeA = createTemple().id();
        long templeB = createTemple().id();
        var event = eventRepository.insert(
                templeA,
                "Festival",
                null,
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                EventStatus.PUBLISHED
        );
        assertThat(eventRepository.findByTempleIdAndId(templeA, event.id())).isPresent();
        assertThat(eventRepository.findByTempleIdAndId(templeB, event.id())).isEmpty();
    }

    @Test
    @Transactional
    void assignmentRequiresExistingAccountAndTemple() {
        long templeId = createTemple().id();
        assertThatThrownBy(() -> assignmentRepository.insert(999_999_999L, templeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void publicEventListingFiltersDraftEvents() {
        long templeId = createTemple().id();
        eventRepository.insert(
                templeId,
                "Draft Event",
                null,
                OffsetDateTime.now().plusDays(4),
                OffsetDateTime.now().plusDays(5),
                EventStatus.DRAFT
        );
        eventRepository.insert(
                templeId,
                "Published Event",
                null,
                OffsetDateTime.now().plusDays(6),
                OffsetDateTime.now().plusDays(7),
                EventStatus.PUBLISHED
        );
        assertThat(eventRepository.findByTempleId(templeId, false, 20, 0)).hasSize(1);
        assertThat(eventRepository.findByTempleId(templeId, true, 20, 0)).hasSize(2);
    }

    @Test
    void flywayTempleMigrationApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = TRUE",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    private long createTempleAdminAccount() {
        return accountRepository.insert(
                "temple-admin-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("ValidPass1234"),
                AccountRole.TEMPLE_ADMIN,
                AccountStatus.ACTIVE
        ).id();
    }

    private com.temple.platform.temple.domain.Temple createTemple() {
        return templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                "Description",
                "City",
                "State",
                "Country",
                "Asia/Kolkata",
                TempleStatus.ACTIVE
        );
    }
}
