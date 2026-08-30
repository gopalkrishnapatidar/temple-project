package com.temple.platform.temple.repository;

import com.temple.platform.temple.domain.EventStatus;
import com.temple.platform.temple.domain.TempleEvent;
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
public class TempleEventRepository {

    private static final RowMapper<TempleEvent> ROW_MAPPER = (rs, rowNum) -> new TempleEvent(
            rs.getLong("id"),
            rs.getLong("temple_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getObject("start_at", OffsetDateTime.class),
            rs.getObject("end_at", OffsetDateTime.class),
            EventStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public TempleEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TempleEvent insert(
            long templeId,
            String name,
            String description,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            EventStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO temple_event (temple_id, name, description, start_at, end_at, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setLong(1, templeId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setObject(4, startAt);
            ps.setObject(5, endAt);
            ps.setString(6, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Temple event insert did not return an id");
        }
        return findByTempleIdAndId(templeId, key.longValue())
                .orElseThrow(() -> new IllegalStateException("Inserted event not found"));
    }

    public Optional<TempleEvent> findByTempleIdAndId(long templeId, long eventId) {
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, name, description, start_at, end_at, status, created_at, updated_at
                FROM temple_event
                WHERE temple_id = ? AND id = ?
                """,
                ROW_MAPPER,
                templeId,
                eventId
        ).stream().findFirst();
    }

    public List<TempleEvent> findByTempleId(long templeId, boolean adminView, int limit, int offset) {
        if (adminView) {
            return jdbcTemplate.query(
                    """
                    SELECT id, temple_id, name, description, start_at, end_at, status, created_at, updated_at
                    FROM temple_event
                    WHERE temple_id = ?
                    ORDER BY start_at ASC, id ASC
                    LIMIT ? OFFSET ?
                    """,
                    ROW_MAPPER,
                    templeId,
                    limit,
                    offset
            );
        }
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, name, description, start_at, end_at, status, created_at, updated_at
                FROM temple_event
                WHERE temple_id = ? AND status = 'PUBLISHED'
                ORDER BY start_at ASC, id ASC
                LIMIT ? OFFSET ?
                """,
                ROW_MAPPER,
                templeId,
                limit,
                offset
        );
    }

    public long countByTempleId(long templeId, boolean adminView) {
        if (adminView) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM temple_event WHERE temple_id = ?",
                    Long.class,
                    templeId
            );
            return count == null ? 0 : count;
        }
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM temple_event
                WHERE temple_id = ? AND status = 'PUBLISHED'
                """,
                Long.class,
                templeId
        );
        return count == null ? 0 : count;
    }

    public boolean update(long templeId, long eventId, UpdateEventFields fields) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (fields.name() != null) {
            sets.add("name = ?");
            args.add(fields.name());
        }
        if (fields.description() != null) {
            sets.add("description = ?");
            args.add(fields.description());
        }
        if (fields.startAt() != null) {
            sets.add("start_at = ?");
            args.add(fields.startAt());
        }
        if (fields.endAt() != null) {
            sets.add("end_at = ?");
            args.add(fields.endAt());
        }
        if (fields.status() != null) {
            sets.add("status = ?");
            args.add(fields.status().name());
        }
        if (sets.isEmpty()) {
            return findByTempleIdAndId(templeId, eventId).isPresent();
        }
        args.add(templeId);
        args.add(eventId);
        String sql = "UPDATE temple_event SET " + String.join(", ", sets)
                + " WHERE temple_id = ? AND id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public record UpdateEventFields(
            String name,
            String description,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            EventStatus status
    ) {
    }
}
