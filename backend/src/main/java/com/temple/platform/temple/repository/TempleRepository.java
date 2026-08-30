package com.temple.platform.temple.repository;

import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
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
public class TempleRepository {

    private static final RowMapper<Temple> ROW_MAPPER = (rs, rowNum) -> new Temple(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("city"),
            rs.getString("state"),
            rs.getString("country"),
            rs.getString("timezone"),
            TempleStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public TempleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Temple insert(
            String name,
            String description,
            String city,
            String state,
            String country,
            String timezone,
            TempleStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO temple (name, description, city, state, country, timezone, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, city);
            ps.setString(4, state);
            ps.setString(5, country);
            ps.setString(6, timezone);
            ps.setString(7, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Temple insert did not return an id");
        }
        return findById(key.longValue()).orElseThrow(() -> new IllegalStateException("Inserted temple not found"));
    }

    public Optional<Temple> findById(long id) {
        return jdbcTemplate.query(
                """
                SELECT id, name, description, city, state, country, timezone, status, created_at, updated_at
                FROM temple
                WHERE id = ?
                """,
                ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public List<Temple> findAllVisible(boolean adminView, long accountId) {
        if (adminView) {
            return jdbcTemplate.query(
                    """
                    SELECT id, name, description, city, state, country, timezone, status, created_at, updated_at
                    FROM temple
                    ORDER BY id
                    """,
                    ROW_MAPPER
            );
        }
        if (accountId > 0) {
            return jdbcTemplate.query(
                    """
                    SELECT t.id, t.name, t.description, t.city, t.state, t.country, t.timezone, t.status,
                           t.created_at, t.updated_at
                    FROM temple t
                    INNER JOIN temple_admin_assignment a ON a.temple_id = t.id
                    WHERE a.account_id = ?
                    ORDER BY t.id
                    """,
                    ROW_MAPPER,
                    accountId
            );
        }
        return jdbcTemplate.query(
                """
                SELECT id, name, description, city, state, country, timezone, status, created_at, updated_at
                FROM temple
                WHERE status = 'ACTIVE'
                ORDER BY id
                """,
                ROW_MAPPER
        );
    }

    public boolean update(long id, UpdateTempleFields fields) {
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
        if (fields.city() != null) {
            sets.add("city = ?");
            args.add(fields.city());
        }
        if (fields.state() != null) {
            sets.add("state = ?");
            args.add(fields.state());
        }
        if (fields.country() != null) {
            sets.add("country = ?");
            args.add(fields.country());
        }
        if (fields.timezone() != null) {
            sets.add("timezone = ?");
            args.add(fields.timezone());
        }
        if (fields.status() != null) {
            sets.add("status = ?");
            args.add(fields.status().name());
        }
        if (sets.isEmpty()) {
            return findById(id).isPresent();
        }
        args.add(id);
        String sql = "UPDATE temple SET " + String.join(", ", sets) + " WHERE id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public record UpdateTempleFields(
            String name,
            String description,
            String city,
            String state,
            String country,
            String timezone,
            TempleStatus status
    ) {
    }
}
