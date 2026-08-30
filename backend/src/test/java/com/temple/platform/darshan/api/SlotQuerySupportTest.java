package com.temple.platform.darshan.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotQuerySupportTest {

    @Test
    void resolvesLocalDateRangeInTempleTimezone() {
        SlotQuerySupport.InstantRange range = SlotQuerySupport.resolveLocalDateRange(
                LocalDate.of(2026, 9, 2),
                "Asia/Kolkata"
        );

        assertThat(range.startInclusive()).isEqualTo(OffsetDateTime.of(2026, 9, 1, 18, 30, 0, 0, ZoneOffset.UTC));
        assertThat(range.endExclusive()).isEqualTo(OffsetDateTime.of(2026, 9, 2, 18, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void rejectsInvalidInstantRange() {
        OffsetDateTime from = OffsetDateTime.now();
        assertThatThrownBy(() -> SlotQuerySupport.resolveInstantRange(from, from))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRangeExceedingMaximum() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusDays(SlotQuerySupport.MAX_RANGE_DAYS + 1);
        assertThatThrownBy(() -> SlotQuerySupport.resolveInstantRange(from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
