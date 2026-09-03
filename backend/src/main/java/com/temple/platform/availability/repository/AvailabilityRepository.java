package com.temple.platform.availability.repository;

import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AvailabilityRepository {

    private static final RowMapper<DarshanSlotAvailabilityRow> DARSHAN_ROW_MAPPER = (rs, rowNum) ->
            new DarshanSlotAvailabilityRow(
                    rs.getLong("slot_id"),
                    rs.getInt("capacity"),
                    DarshanSlotStatus.valueOf(rs.getString("status")),
                    rs.getObject("end_at", OffsetDateTime.class),
                    rs.getInt("booked_quantity")
            );

    private static final RowMapper<RitualSlotAvailabilityRow> RITUAL_ROW_MAPPER = (rs, rowNum) ->
            new RitualSlotAvailabilityRow(
                    rs.getLong("slot_id"),
                    rs.getInt("capacity"),
                    RitualSlotStatus.valueOf(rs.getString("status")),
                    Timestamptz.toInstant(rs, "end_at"),
                    rs.getInt("booked_quantity")
            );

    private final JdbcTemplate jdbcTemplate;

    public AvailabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DarshanSlotAvailabilityRow> findDarshanSlotAvailability(long darshanId, long slotId) {
        return jdbcTemplate.query(
                """
                SELECT ds.id AS slot_id,
                       ds.capacity,
                       ds.status,
                       ds.end_at,
                       COALESCE(SUM(b.quantity), 0)::int AS booked_quantity
                FROM darshan_slot ds
                LEFT JOIN booking b
                       ON b.darshan_slot_id = ds.id AND b.status = 'CONFIRMED'
                WHERE ds.darshan_id = ? AND ds.id = ?
                GROUP BY ds.id, ds.capacity, ds.status, ds.end_at
                """,
                DARSHAN_ROW_MAPPER,
                darshanId,
                slotId
        ).stream().findFirst();
    }

    public List<DarshanSlotAvailabilityRow> findDarshanSlotAvailabilities(
            long darshanId,
            boolean adminView,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT ds.id AS slot_id,
                       ds.capacity,
                       ds.status,
                       ds.end_at,
                       COALESCE(SUM(b.quantity), 0)::int AS booked_quantity
                FROM darshan_slot ds
                LEFT JOIN booking b
                       ON b.darshan_slot_id = ds.id AND b.status = 'CONFIRMED'
                WHERE ds.darshan_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(darshanId);
        appendDarshanVisibilityFilter(sql, args, adminView);
        appendDarshanRangeFilter(sql, args, rangeStart, rangeEnd);
        sql.append("""
                 GROUP BY ds.id, ds.capacity, ds.status, ds.end_at, ds.start_at
                 ORDER BY ds.start_at ASC, ds.id ASC
                 LIMIT ? OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), DARSHAN_ROW_MAPPER, args.toArray());
    }

    public long countDarshanSlotAvailabilities(
            long darshanId,
            boolean adminView,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM darshan_slot ds WHERE ds.darshan_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(darshanId);
        appendDarshanVisibilityFilter(sql, args, adminView);
        appendDarshanRangeFilter(sql, args, rangeStart, rangeEnd);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public Optional<RitualSlotAvailabilityRow> findRitualSlotAvailability(long ritualId, long slotId) {
        return jdbcTemplate.query(
                """
                SELECT rs.id AS slot_id,
                       rs.capacity,
                       rs.status,
                       rs.end_at,
                       COALESCE(SUM(b.quantity), 0)::int AS booked_quantity
                FROM ritual_slot rs
                LEFT JOIN booking b
                       ON b.ritual_slot_id = rs.id AND b.status = 'CONFIRMED'
                WHERE rs.ritual_id = ? AND rs.id = ?
                GROUP BY rs.id, rs.capacity, rs.status, rs.end_at
                """,
                RITUAL_ROW_MAPPER,
                ritualId,
                slotId
        ).stream().findFirst();
    }

    public List<RitualSlotAvailabilityRow> findRitualSlotAvailabilities(
            long ritualId,
            boolean adminView,
            Instant rangeStart,
            Instant rangeEnd,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT rs.id AS slot_id,
                       rs.capacity,
                       rs.status,
                       rs.end_at,
                       COALESCE(SUM(b.quantity), 0)::int AS booked_quantity
                FROM ritual_slot rs
                LEFT JOIN booking b
                       ON b.ritual_slot_id = rs.id AND b.status = 'CONFIRMED'
                WHERE rs.ritual_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(ritualId);
        appendRitualVisibilityFilter(sql, args, adminView);
        appendRitualRangeFilter(sql, args, rangeStart, rangeEnd);
        sql.append("""
                 GROUP BY rs.id, rs.capacity, rs.status, rs.end_at, rs.start_at
                 ORDER BY rs.start_at ASC, rs.id ASC
                 LIMIT ? OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), RITUAL_ROW_MAPPER, args.toArray());
    }

    public long countRitualSlotAvailabilities(
            long ritualId,
            boolean adminView,
            Instant rangeStart,
            Instant rangeEnd) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ritual_slot rs WHERE rs.ritual_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(ritualId);
        appendRitualVisibilityFilter(sql, args, adminView);
        appendRitualRangeFilter(sql, args, rangeStart, rangeEnd);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private static void appendDarshanVisibilityFilter(StringBuilder sql, List<Object> args, boolean adminView) {
        if (!adminView) {
            sql.append(" AND ds.status = 'AVAILABLE' AND ds.end_at > ?");
            args.add(OffsetDateTime.now());
        }
    }

    private static void appendDarshanRangeFilter(
            StringBuilder sql,
            List<Object> args,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND ds.start_at < ? AND ds.end_at > ?");
            args.add(rangeEnd);
            args.add(rangeStart);
        }
    }

    private static void appendRitualVisibilityFilter(StringBuilder sql, List<Object> args, boolean adminView) {
        if (!adminView) {
            sql.append(" AND rs.status = 'AVAILABLE' AND rs.end_at > ?");
            args.add(Timestamptz.toOffsetDateTime(Instant.now()));
        }
    }

    private static void appendRitualRangeFilter(
            StringBuilder sql,
            List<Object> args,
            Instant rangeStart,
            Instant rangeEnd) {
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND rs.start_at < ? AND rs.end_at > ?");
            args.add(Timestamptz.toOffsetDateTime(rangeEnd));
            args.add(Timestamptz.toOffsetDateTime(rangeStart));
        }
    }

    public record DarshanSlotAvailabilityRow(
            long slotId,
            int capacity,
            DarshanSlotStatus status,
            OffsetDateTime endAt,
            int bookedQuantity
    ) {
    }

    public record RitualSlotAvailabilityRow(
            long slotId,
            int capacity,
            RitualSlotStatus status,
            Instant endAt,
            int bookedQuantity
    ) {
    }

    private static final class Timestamptz {

        private Timestamptz() {
        }

        private static Instant toInstant(ResultSet rs, String column) throws SQLException {
            OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
            return value == null ? null : value.toInstant();
        }

        private static OffsetDateTime toOffsetDateTime(Instant instant) {
            return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
        }
    }
}
