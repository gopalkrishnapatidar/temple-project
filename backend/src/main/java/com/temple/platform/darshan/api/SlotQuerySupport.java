package com.temple.platform.darshan.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class SlotQuerySupport {

    public static final int MAX_RANGE_DAYS = 90;

    private SlotQuerySupport() {
    }

    public record InstantRange(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {
    }

    public static InstantRange resolveLocalDateRange(LocalDate date, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime dayStart = date.atStartOfDay(zoneId);
        ZonedDateTime dayEnd = date.plusDays(1).atStartOfDay(zoneId);
        return new InstantRange(dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime());
    }

    public static InstantRange resolveInstantRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("slot query range exceeds maximum of " + MAX_RANGE_DAYS + " days");
        }
        return new InstantRange(from, to);
    }
}
