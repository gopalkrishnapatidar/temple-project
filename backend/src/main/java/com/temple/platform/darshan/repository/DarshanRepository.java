package com.temple.platform.darshan.repository;

import com.temple.platform.darshan.domain.Darshan;
import com.temple.platform.darshan.domain.DarshanStatus;
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
public class DarshanRepository {

    private static final RowMapper<Darshan> ROW_MAPPER = (rs, rowNum) -> new Darshan(
            rs.getLong("id"),
            rs.getLong("temple_id"),
            rs.getString("name"),
            rs.getString("description"),
            DarshanStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public DarshanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Darshan insert(long templeId, String name, String description, DarshanStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO darshan (temple_id, name, description, status)
                    VALUES (?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setLong(1, templeId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Darshan insert did not return an id");
        }
        return findByTempleIdAndId(templeId, key.longValue())
                .orElseThrow(() -> new IllegalStateException("Inserted darshan not found"));
    }

    public Optional<Darshan> findByTempleIdAndId(long templeId, long darshanId) {
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, name, description, status, created_at, updated_at
                FROM darshan
                WHERE temple_id = ? AND id = ?
                """,
                ROW_MAPPER,
                templeId,
                darshanId
        ).stream().findFirst();
    }

    public Optional<Darshan> findById(long darshanId) {
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, name, description, status, created_at, updated_at
                FROM darshan
                WHERE id = ?
                """,
                ROW_MAPPER,
                darshanId
        ).stream().findFirst();
    }

    public List<Darshan> findByTempleId(long templeId, boolean adminView) {
        if (adminView) {
            return jdbcTemplate.query(
                    """
                    SELECT id, temple_id, name, description, status, created_at, updated_at
                    FROM darshan
                    WHERE temple_id = ?
                    ORDER BY name ASC, id ASC
                    """,
                    ROW_MAPPER,
                    templeId
            );
        }
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, name, description, status, created_at, updated_at
                FROM darshan
                WHERE temple_id = ? AND status = 'ACTIVE'
                ORDER BY name ASC, id ASC
                """,
                ROW_MAPPER,
                templeId
        );
    }

    public boolean update(long templeId, long darshanId, UpdateDarshanFields fields) {
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
        if (fields.status() != null) {
            sets.add("status = ?");
            args.add(fields.status().name());
        }
        if (sets.isEmpty()) {
            return findByTempleIdAndId(templeId, darshanId).isPresent();
        }
        args.add(templeId);
        args.add(darshanId);
        String sql = "UPDATE darshan SET " + String.join(", ", sets)
                + " WHERE temple_id = ? AND id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public record UpdateDarshanFields(
            String name,
            String description,
            DarshanStatus status
    ) {
    }
}
