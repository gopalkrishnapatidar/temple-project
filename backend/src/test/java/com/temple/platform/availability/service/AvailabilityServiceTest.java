package com.temple.platform.availability.service;

import com.temple.platform.availability.api.dto.SlotAvailabilityResponse;
import com.temple.platform.availability.repository.AvailabilityRepository.DarshanSlotAvailabilityRow;
import com.temple.platform.availability.repository.AvailabilityRepository.RitualSlotAvailabilityRow;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityServiceTest {

    @Test
    void noBookingsShowsFullAvailability() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        SlotAvailabilityResponse response = AvailabilityService.toDarshanResponse(
                new DarshanSlotAvailabilityRow(
                        1L,
                        10,
                        DarshanSlotStatus.AVAILABLE,
                        OffsetDateTime.of(2026, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC),
                        0),
                now);

        assertThat(response.capacity()).isEqualTo(10);
        assertThat(response.bookedQuantity()).isZero();
        assertThat(response.remainingCapacity()).isEqualTo(10);
        assertThat(response.available()).isTrue();
    }

    @Test
    void confirmedBookingsReduceRemainingCapacity() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        SlotAvailabilityResponse response = AvailabilityService.toDarshanResponse(
                new DarshanSlotAvailabilityRow(
                        1L,
                        10,
                        DarshanSlotStatus.AVAILABLE,
                        OffsetDateTime.of(2026, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC),
                        3),
                now);

        assertThat(response.bookedQuantity()).isEqualTo(3);
        assertThat(response.remainingCapacity()).isEqualTo(7);
        assertThat(response.available()).isTrue();
    }

    @Test
    void fullSlotIsUnavailable() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        SlotAvailabilityResponse response = AvailabilityService.toRitualResponse(
                new RitualSlotAvailabilityRow(
                        2L,
                        5,
                        RitualSlotStatus.AVAILABLE,
                        Instant.parse("2026-01-02T12:00:00Z"),
                        5),
                now);

        assertThat(response.remainingCapacity()).isZero();
        assertThat(response.available()).isFalse();
    }

    @Test
    void overCapacityNeverExposesNegativeRemainingCapacity() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        SlotAvailabilityResponse response = AvailabilityService.toDarshanResponse(
                new DarshanSlotAvailabilityRow(
                        3L,
                        2,
                        DarshanSlotStatus.AVAILABLE,
                        OffsetDateTime.of(2026, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC),
                        5),
                now);

        assertThat(response.remainingCapacity()).isZero();
        assertThat(response.available()).isFalse();
    }

    @Test
    void cancelledOrPastSlotsAreNotAvailable() {
        Instant now = Instant.parse("2026-01-03T10:00:00Z");
        SlotAvailabilityResponse cancelled = AvailabilityService.toDarshanResponse(
                new DarshanSlotAvailabilityRow(
                        4L,
                        10,
                        DarshanSlotStatus.CANCELLED,
                        OffsetDateTime.of(2026, 1, 5, 12, 0, 0, 0, ZoneOffset.UTC),
                        0),
                now);
        SlotAvailabilityResponse past = AvailabilityService.toRitualResponse(
                new RitualSlotAvailabilityRow(
                        5L,
                        10,
                        RitualSlotStatus.AVAILABLE,
                        Instant.parse("2026-01-02T12:00:00Z"),
                        0),
                now);

        assertThat(cancelled.available()).isFalse();
        assertThat(past.available()).isFalse();
    }
}
