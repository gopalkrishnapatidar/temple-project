package com.temple.platform.ritual.repository;

import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class RitualRepositoryTest {

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private RitualRepository ritualRepository;

    @Autowired
    private RitualSlotRepository slotRepository;

    @Test
    @Transactional
    void zeroPriceAllowedAndNegativePriceRejected() {
        long templeId = createTemple();
        Ritual ritual = ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Free ritual",
                null,
                30,
                BigDecimal.ZERO,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        );
        assertThat(ritual.price().compareTo(BigDecimal.ZERO)).isZero();

        assertThatThrownBy(() -> ritualRepository.insert(
                templeId,
                RitualType.HAVAN,
                "Negative",
                null,
                30,
                new BigDecimal("-0.01"),
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void zeroDurationRejectedByDatabase() {
        long templeId = createTemple();
        assertThatThrownBy(() -> ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Invalid duration",
                null,
                0,
                BigDecimal.ZERO,
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void overlappingAvailableSlotsAllowed() {
        long ritualId = insertRitual(createTemple());
        Instant start = Instant.now().plus(Duration.ofDays(5));
        Instant middle = start.plus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));

        slotRepository.insert(ritualId, start, middle, 10, RitualSlotStatus.AVAILABLE);
        slotRepository.insert(ritualId, middle.minus(Duration.ofMinutes(30)), end, 10, RitualSlotStatus.AVAILABLE);
    }

    @Test
    @Transactional
    void slotEndMustBeAfterStart() {
        long ritualId = insertRitual(createTemple());
        Instant start = Instant.now().plus(Duration.ofDays(6));

        assertThatThrownBy(() -> slotRepository.insert(ritualId, start, start, 10, RitualSlotStatus.AVAILABLE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void zeroCapacityRejectedByDatabase() {
        long ritualId = insertRitual(createTemple());
        Instant start = Instant.now().plus(Duration.ofDays(7));

        assertThatThrownBy(() -> slotRepository.insert(ritualId, start, start.plus(Duration.ofHours(1)), 0, RitualSlotStatus.AVAILABLE))
                .isInstanceOf(DataIntegrityViolationException.class);
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

    private long insertRitual(long templeId) {
        return ritualRepository.insert(
                templeId,
                RitualType.PUJA,
                "Ritual " + UUID.randomUUID(),
                null,
                30,
                new BigDecimal("10.00"),
                RitualCurrency.INR,
                RitualStatus.ACTIVE
        ).id();
    }
}
