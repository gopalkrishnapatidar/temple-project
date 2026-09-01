package com.temple.platform.ritual.repository;

import com.temple.platform.ritual.domain.RitualSlot;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RitualSlotRepository {

    private static final RowMapper<RitualSlot> ROW_MAPPER = (rs, rowNum) -> new RitualSlot(
            rs.getLong("id"),
            rs.getLong("ritual_id"),
            TimestamptzMapping.toInstant(rs, "start_at"),
            TimestamptzMapping.toInstant(rs, "end_at"),
            rs.getInt("capacity"),
            RitualSlotStatus.valueOf(rs.getString("status")),
            TimestamptzMapping.toInstant(rs, "created_at"),
            TimestamptzMapping.toInstant(rs, "updated_at")
    );

    private final JdbcTemplate jdbcTemplate;

    public RitualSlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RitualSlot insert(
            long ritualId,
            Instant startAt,
            Instant endAt,
            int capacity,
            RitualSlotStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO ritual_slot (ritual_id, start_at, end_at, capacity, status)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setLong(1, ritualId);
            ps.setObject(2, TimestamptzMapping.toOffsetDateTime(startAt));
            ps.setObject(3, TimestamptzMapping.toOffsetDateTime(endAt));
            ps.setInt(4, capacity);
            ps.setString(5, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Ritual slot insert did not return an id");
        }
        return findByRitualIdAndId(ritualId, key.longValue())
                .orElseThrow(() -> new IllegalStateException("Inserted ritual slot not found"));
    }

    public Optional<RitualSlot> findByRitualIdAndId(long ritualId, long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, ritual_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM ritual_slot
                WHERE ritual_id = ? AND id = ?
                """,
                ROW_MAPPER,
                ritualId,
                slotId
        ).stream().findFirst();
    }

    public List<RitualSlot> findByRitualId(
            long ritualId,
            boolean adminView,
            Instant rangeStart,
            Instant rangeEnd,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, ritual_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM ritual_slot
                WHERE ritual_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(ritualId);
        if (!adminView) {
            sql.append(" AND status = 'AVAILABLE' AND end_at > ?");
            args.add(TimestamptzMapping.toOffsetDateTime(Instant.now()));
        }
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND start_at < ? AND end_at > ?");
            args.add(TimestamptzMapping.toOffsetDateTime(rangeEnd));
            args.add(TimestamptzMapping.toOffsetDateTime(rangeStart));
        }
        sql.append(" ORDER BY start_at ASC, id ASC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long countByRitualId(
            long ritualId,
            boolean adminView,
            Instant rangeStart,
            Instant rangeEnd) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ritual_slot WHERE ritual_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(ritualId);
        if (!adminView) {
            sql.append(" AND status = 'AVAILABLE' AND end_at > ?");
            args.add(TimestamptzMapping.toOffsetDateTime(Instant.now()));
        }
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND start_at < ? AND end_at > ?");
            args.add(TimestamptzMapping.toOffsetDateTime(rangeEnd));
            args.add(TimestamptzMapping.toOffsetDateTime(rangeStart));
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public boolean update(long ritualId, long slotId, UpdateSlotFields fields) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (fields.startAt() != null) {
            sets.add("start_at = ?");
            args.add(TimestamptzMapping.toOffsetDateTime(fields.startAt()));
        }
        if (fields.endAt() != null) {
            sets.add("end_at = ?");
            args.add(TimestamptzMapping.toOffsetDateTime(fields.endAt()));
        }
        if (fields.capacity() != null) {
            sets.add("capacity = ?");
            args.add(fields.capacity());
        }
        if (fields.status() != null) {
            sets.add("status = ?");
            args.add(fields.status().name());
        }
        if (sets.isEmpty()) {
            return findByRitualIdAndId(ritualId, slotId).isPresent();
        }
        args.add(ritualId);
        args.add(slotId);
        String sql = "UPDATE ritual_slot SET " + String.join(", ", sets)
                + " WHERE ritual_id = ? AND id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public Optional<RitualSlot> findById(long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, ritual_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM ritual_slot
                WHERE id = ?
                """,
                ROW_MAPPER,
                slotId
        ).stream().findFirst();
    }

    public Optional<RitualSlot> lockById(long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, ritual_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM ritual_slot
                WHERE id = ?
                FOR UPDATE
                """,
                ROW_MAPPER,
                slotId
        ).stream().findFirst();
    }

    public record UpdateSlotFields(
            Instant startAt,
            Instant endAt,
            Integer capacity,
            RitualSlotStatus status
    ) {
    }
}
