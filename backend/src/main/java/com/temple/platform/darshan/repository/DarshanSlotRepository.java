package com.temple.platform.darshan.repository;

import com.temple.platform.darshan.domain.DarshanSlot;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DarshanSlotRepository {

    private static final RowMapper<DarshanSlot> ROW_MAPPER = (rs, rowNum) -> new DarshanSlot(
            rs.getLong("id"),
            rs.getLong("darshan_id"),
            rs.getObject("start_at", OffsetDateTime.class),
            rs.getObject("end_at", OffsetDateTime.class),
            rs.getInt("capacity"),
            DarshanSlotStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public DarshanSlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DarshanSlot insert(
            long darshanId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int capacity,
            DarshanSlotStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO darshan_slot (darshan_id, start_at, end_at, capacity, status)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setLong(1, darshanId);
            ps.setObject(2, startAt);
            ps.setObject(3, endAt);
            ps.setInt(4, capacity);
            ps.setString(5, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Darshan slot insert did not return an id");
        }
        return findByDarshanIdAndId(darshanId, key.longValue())
                .orElseThrow(() -> new IllegalStateException("Inserted darshan slot not found"));
    }

    public Optional<DarshanSlot> findById(long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, darshan_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM darshan_slot
                WHERE id = ?
                """,
                ROW_MAPPER,
                slotId
        ).stream().findFirst();
    }

    public Optional<DarshanSlot> lockById(long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, darshan_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM darshan_slot
                WHERE id = ?
                FOR UPDATE
                """,
                ROW_MAPPER,
                slotId
        ).stream().findFirst();
    }

    public Optional<DarshanSlot> findByDarshanIdAndId(long darshanId, long slotId) {
        return jdbcTemplate.query(
                """
                SELECT id, darshan_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM darshan_slot
                WHERE darshan_id = ? AND id = ?
                """,
                ROW_MAPPER,
                darshanId,
                slotId
        ).stream().findFirst();
    }

    public List<DarshanSlot> findByDarshanId(
            long darshanId,
            boolean adminView,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, darshan_id, start_at, end_at, capacity, status, created_at, updated_at
                FROM darshan_slot
                WHERE darshan_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(darshanId);
        if (!adminView) {
            sql.append(" AND status = 'AVAILABLE' AND end_at > ?");
            args.add(OffsetDateTime.now());
        }
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND start_at < ? AND end_at > ?");
            args.add(rangeEnd);
            args.add(rangeStart);
        }
        sql.append(" ORDER BY start_at ASC, id ASC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long countByDarshanId(
            long darshanId,
            boolean adminView,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM darshan_slot WHERE darshan_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(darshanId);
        if (!adminView) {
            sql.append(" AND status = 'AVAILABLE' AND end_at > ?");
            args.add(OffsetDateTime.now());
        }
        if (rangeStart != null && rangeEnd != null) {
            sql.append(" AND start_at < ? AND end_at > ?");
            args.add(rangeEnd);
            args.add(rangeStart);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public boolean update(long darshanId, long slotId, UpdateSlotFields fields) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (fields.startAt() != null) {
            sets.add("start_at = ?");
            args.add(fields.startAt());
        }
        if (fields.endAt() != null) {
            sets.add("end_at = ?");
            args.add(fields.endAt());
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
            return findByDarshanIdAndId(darshanId, slotId).isPresent();
        }
        args.add(darshanId);
        args.add(slotId);
        String sql = "UPDATE darshan_slot SET " + String.join(", ", sets)
                + " WHERE darshan_id = ? AND id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public record UpdateSlotFields(
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            Integer capacity,
            DarshanSlotStatus status
    ) {
    }
}
