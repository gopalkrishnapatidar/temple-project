package com.temple.platform.booking.repository;

import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
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
import com.temple.platform.temple.repository.TempleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class BookingRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountRepository accountRepository;

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

    @Test
    @Transactional
    void quantityMustBePositive() {
        Fixture fixture = createFixture();
        assertThatThrownBy(() -> insertRaw(
                UUID.randomUUID(),
                fixture.accountId(),
                fixture.darshanSlotId(),
                null,
                0,
                "CONFIRMED",
                UUID.randomUUID().toString()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void bothSlotFksSetRejectedByDatabase() {
        Fixture fixture = createFixture();
        assertThatThrownBy(() -> insertRaw(
                UUID.randomUUID(),
                fixture.accountId(),
                fixture.darshanSlotId(),
                fixture.ritualSlotId(),
                1,
                "CONFIRMED",
                UUID.randomUUID().toString()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void neitherSlotFkSetRejectedByDatabase() {
        Fixture fixture = createFixture();
        assertThatThrownBy(() -> insertRaw(
                UUID.randomUUID(),
                fixture.accountId(),
                null,
                null,
                1,
                "CONFIRMED",
                UUID.randomUUID().toString()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void statusMustBeConfirmedOrCancelled() {
        Fixture fixture = createFixture();
        assertThatThrownBy(() -> insertRaw(
                UUID.randomUUID(),
                fixture.accountId(),
                fixture.darshanSlotId(),
                null,
                1,
                "PENDING",
                UUID.randomUUID().toString()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void bookingReferenceIsUnique() {
        Fixture fixture = createFixture();
        UUID reference = UUID.randomUUID();
        bookingRepository.insertIgnoringIdempotencyConflict(
                reference,
                fixture.accountId(),
                fixture.darshanSlotId(),
                null,
                1,
                BookingStatus.CONFIRMED,
                UUID.randomUUID().toString()
        );

        assertThatThrownBy(() -> insertRaw(
                reference,
                fixture.accountId(),
                null,
                fixture.ritualSlotId(),
                1,
                "CONFIRMED",
                UUID.randomUUID().toString()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void sameAccountIdempotencyKeyDoesNotInsertTwice() {
        Fixture fixture = createFixture();
        String key = UUID.randomUUID().toString();
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                fixture.accountId(),
                fixture.darshanSlotId(),
                null,
                1,
                BookingStatus.CONFIRMED,
                key
        );

        var ignored = bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                fixture.accountId(),
                null,
                fixture.ritualSlotId(),
                1,
                BookingStatus.CONFIRMED,
                key
        );
        assertThat(ignored).isEmpty();
    }

    private void insertRaw(
            UUID reference,
            long accountId,
            Long darshanSlotId,
            Long ritualSlotId,
            int quantity,
            String status,
            String idempotencyKey) {
        jdbcTemplate.update(
                """
                INSERT INTO booking (
                    booking_reference, account_id, darshan_slot_id, ritual_slot_id,
                    quantity, status, idempotency_key
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                reference,
                accountId,
                darshanSlotId,
                ritualSlotId,
                quantity,
                status,
                idempotencyKey
        );
    }

    private Fixture createFixture() {
        long accountId = accountRepository.insert(
                "booking-repo-" + UUID.randomUUID() + "@example.com",
                "hash",
                AccountRole.DEVOTEE,
                AccountStatus.ACTIVE
        ).id();
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        long darshanId = darshanRepository.insert(templeId, "Darshan", null, DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(10);
        long darshanSlotId = darshanSlotRepository.insert(
                darshanId, start, start.plusHours(1), 10, DarshanSlotStatus.AVAILABLE).id();
        long ritualId = ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Puja",
                null,
                30,
                BigDecimal.TEN,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
        Instant ritualStart = Instant.now().plus(Duration.ofDays(11));
        long ritualSlotId = ritualSlotRepository.insert(
                ritualId, ritualStart, ritualStart.plus(Duration.ofHours(1)), 8, RitualSlotStatus.AVAILABLE).id();
        return new Fixture(accountId, darshanSlotId, ritualSlotId);
    }

    private record Fixture(long accountId, long darshanSlotId, long ritualSlotId) {
    }
}
