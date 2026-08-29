package com.temple.platform.platform.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class ApplicationMetadataRepository {

    private final JdbcTemplate jdbcTemplate;

    public ApplicationMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findValue(String key) {
        return jdbcTemplate.query(
                "SELECT value FROM application_metadata WHERE key = ?",
                rs -> rs.next() ? Optional.of(rs.getString("value")) : Optional.empty(),
                key
        );
    }

    public Optional<String> findLatestFlywayVersion() {
        return jdbcTemplate.query(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                rs -> rs.next() ? Optional.of(rs.getString("version")) : Optional.empty()
        );
    }

    public OffsetDateTime findUpdatedAt(String key) {
        return jdbcTemplate.query(
                "SELECT updated_at FROM application_metadata WHERE key = ?",
                rs -> rs.next() ? rs.getObject("updated_at", OffsetDateTime.class) : null,
                key
        );
    }

    public void updateValue(String key, String value) {
        int updated = jdbcTemplate.update(
                "UPDATE application_metadata SET value = ? WHERE key = ?",
                value,
                key
        );
        if (updated != 1) {
            throw new IllegalStateException("Expected one metadata row for key: " + key);
        }
    }

    public void insert(String key, String value) {
        jdbcTemplate.update(
                "INSERT INTO application_metadata (key, value) VALUES (?, ?)",
                key,
                value
        );
    }
}
