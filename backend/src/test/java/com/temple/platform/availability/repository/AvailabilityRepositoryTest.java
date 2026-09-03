package com.temple.platform.availability.repository;

import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.repository.BookingRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AvailabilityRepositoryTest {

    @Autowired
    private AvailabilityRepository availabilityRepository;

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
    private AccountRepository accountRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void darshanAggregateQueryCombinesConfirmedBookings() {
        long darshanId = createDarshan();
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);
        long slotOne = darshanSlotRepository.insert(
                darshanId, start, start.plusHours(1), 10, DarshanSlotStatus.AVAILABLE).id();
        long slotTwo = darshanSlotRepository.insert(
                darshanId, start.plusHours(2), start.plusHours(3), 8, DarshanSlotStatus.AVAILABLE).id();
        long accountId = createAccount();

        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, slotOne, null, 2, BookingStatus.CONFIRMED, "k1");
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, slotOne, null, 1, BookingStatus.CONFIRMED, "k2");
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, slotOne, null, 4, BookingStatus.CANCELLED, "k3");
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, slotTwo, null, 3, BookingStatus.CONFIRMED, "k4");

        List<AvailabilityRepository.DarshanSlotAvailabilityRow> rows = availabilityRepository
                .findDarshanSlotAvailabilities(darshanId, true, null, null, 10, 0);

        assertThat(rows).hasSize(2);
        AvailabilityRepository.DarshanSlotAvailabilityRow first = rows.stream()
                .filter(row -> row.slotId() == slotOne)
                .findFirst()
                .orElseThrow();
        AvailabilityRepository.DarshanSlotAvailabilityRow second = rows.stream()
                .filter(row -> row.slotId() == slotTwo)
                .findFirst()
                .orElseThrow();
        assertThat(first.bookedQuantity()).isEqualTo(3);
        assertThat(second.bookedQuantity()).isEqualTo(3);
    }

    @Test
    void ritualAggregateQueryCombinesConfirmedBookings() {
        long ritualId = createRitual();
        Instant start = Instant.now().plus(Duration.ofDays(4));
        long slotId = ritualSlotRepository.insert(
                ritualId, start, start.plus(Duration.ofHours(1)), 6, RitualSlotStatus.AVAILABLE).id();
        long accountId = createAccount();

        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, null, slotId, 2, BookingStatus.CONFIRMED, "r1");
        bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(), accountId, null, slotId, 1, BookingStatus.CANCELLED, "r2");

        AvailabilityRepository.RitualSlotAvailabilityRow row = availabilityRepository
                .findRitualSlotAvailability(ritualId, slotId)
                .orElseThrow();

        assertThat(row.capacity()).isEqualTo(6);
        assertThat(row.bookedQuantity()).isEqualTo(2);
    }

    private long createDarshan() {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        return darshanRepository.insert(templeId, "Darshan " + UUID.randomUUID(), null, DarshanStatus.ACTIVE).id();
    }

    private long createRitual() {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        return ritualRepository.insert(
                templeId,
                RitualType.HAVAN,
                "Havan " + UUID.randomUUID(),
                null,
                45,
                BigDecimal.ONE,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
    }

    private long createAccount() {
        return accountRepository.insert(
                "devotee-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("ValidPass1234"),
                AccountRole.DEVOTEE,
                AccountStatus.ACTIVE
        ).id();
    }
}
