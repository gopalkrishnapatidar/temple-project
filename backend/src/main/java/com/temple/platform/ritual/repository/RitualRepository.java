package com.temple.platform.ritual.repository;

import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RitualRepository {

    private static final RowMapper<Ritual> ROW_MAPPER = (rs, rowNum) -> new Ritual(
            rs.getLong("id"),
            rs.getLong("temple_id"),
            RitualType.valueOf(rs.getString("type")),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("duration_minutes"),
            rs.getBigDecimal("price"),
            RitualCurrency.valueOf(rs.getString("currency")),
            RitualStatus.valueOf(rs.getString("status")),
            TimestamptzMapping.toInstant(rs, "created_at"),
            TimestamptzMapping.toInstant(rs, "updated_at")
    );

    private final JdbcTemplate jdbcTemplate;

    public RitualRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Ritual insert(
            long templeId,
            RitualType type,
            String name,
            String description,
            int durationMinutes,
            BigDecimal price,
            RitualCurrency currency,
            RitualStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO ritual (
                        temple_id, type, name, description, duration_minutes, price, currency, status
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setLong(1, templeId);
            ps.setString(2, type.name());
            ps.setString(3, name);
            ps.setString(4, description);
            ps.setInt(5, durationMinutes);
            ps.setBigDecimal(6, price);
            ps.setString(7, currency.name());
            ps.setString(8, status.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Ritual insert did not return an id");
        }
        return findByTempleIdAndId(templeId, key.longValue())
                .orElseThrow(() -> new IllegalStateException("Inserted ritual not found"));
    }

    public Optional<Ritual> findByTempleIdAndId(long templeId, long ritualId) {
        return jdbcTemplate.query(
                """
                SELECT id, temple_id, type, name, description, duration_minutes, price, currency, status,
                       created_at, updated_at
                FROM ritual
                WHERE temple_id = ? AND id = ?
                """,
                ROW_MAPPER,
                templeId,
                ritualId
        ).stream().findFirst();
    }

    public List<Ritual> findByTempleId(
            long templeId,
            RitualType type,
            boolean adminView,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, temple_id, type, name, description, duration_minutes, price, currency, status,
                       created_at, updated_at
                FROM ritual
                WHERE temple_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(templeId);
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type.name());
        }
        if (!adminView) {
            sql.append(" AND status = 'ACTIVE'");
        }
        sql.append(" ORDER BY name ASC, id ASC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long countByTempleId(long templeId, RitualType type, boolean adminView) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ritual WHERE temple_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(templeId);
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type.name());
        }
        if (!adminView) {
            sql.append(" AND status = 'ACTIVE'");
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public boolean update(long templeId, long ritualId, UpdateRitualFields fields) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (fields.type() != null) {
            sets.add("type = ?");
            args.add(fields.type().name());
        }
        if (fields.name() != null) {
            sets.add("name = ?");
            args.add(fields.name());
        }
        if (fields.description() != null) {
            sets.add("description = ?");
            args.add(fields.description());
        }
        if (fields.durationMinutes() != null) {
            sets.add("duration_minutes = ?");
            args.add(fields.durationMinutes());
        }
        if (fields.price() != null) {
            sets.add("price = ?");
            args.add(fields.price());
        }
        if (fields.currency() != null) {
            sets.add("currency = ?");
            args.add(fields.currency().name());
        }
        if (fields.status() != null) {
            sets.add("status = ?");
            args.add(fields.status().name());
        }
        if (sets.isEmpty()) {
            return findByTempleIdAndId(templeId, ritualId).isPresent();
        }
        args.add(templeId);
        args.add(ritualId);
        String sql = "UPDATE ritual SET " + String.join(", ", sets)
                + " WHERE temple_id = ? AND id = ?";
        return jdbcTemplate.update(sql, args.toArray()) == 1;
    }

    public record UpdateRitualFields(
            RitualType type,
            String name,
            String description,
            Integer durationMinutes,
            BigDecimal price,
            RitualCurrency currency,
            RitualStatus status
    ) {
    }
}
