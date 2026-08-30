package com.temple.platform.darshan.repository;

import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.repository.TempleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class DarshanSlotRepositoryTest {

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private DarshanRepository darshanRepository;

    @Autowired
    private DarshanSlotRepository slotRepository;

    @Test
    @Transactional
    void overlappingAvailableSlotsRejectedByDatabase() {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        long darshanId = darshanRepository.insert(templeId, "Morning", null, com.temple.platform.darshan.domain.DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        OffsetDateTime middle = start.plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        slotRepository.insert(darshanId, start, middle, 50, DarshanSlotStatus.AVAILABLE);

        assertThatThrownBy(() -> slotRepository.insert(darshanId, middle.minusMinutes(30), end, 50, DarshanSlotStatus.AVAILABLE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void adjacentAvailableSlotsAllowed() {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        long darshanId = darshanRepository.insert(templeId, "Morning", null, com.temple.platform.darshan.domain.DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(6);
        OffsetDateTime middle = start.plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        slotRepository.insert(darshanId, start, middle, 50, DarshanSlotStatus.AVAILABLE);
        slotRepository.insert(darshanId, middle, end, 50, DarshanSlotStatus.AVAILABLE);
    }

    @Test
    @Transactional
    void zeroCapacityRejectedByDatabase() {
        long templeId = templeRepository.insert(
                "Temple " + UUID.randomUUID(),
                null,
                "City",
                "State",
                "Country",
                "UTC",
                TempleStatus.ACTIVE
        ).id();
        long darshanId = darshanRepository.insert(templeId, "Morning", null, com.temple.platform.darshan.domain.DarshanStatus.ACTIVE).id();
        OffsetDateTime start = OffsetDateTime.now().plusDays(7);
        OffsetDateTime end = start.plusHours(1);

        assertThatThrownBy(() -> slotRepository.insert(darshanId, start, end, 0, DarshanSlotStatus.AVAILABLE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
