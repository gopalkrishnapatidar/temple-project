package com.temple.platform.notification.repository;

import com.temple.platform.notification.domain.AggregateType;
import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, event_id, aggregate_type, aggregate_reference, event_type, event_version,
                   payload::text AS payload_json, created_at, published_at, publish_attempts, last_error
            FROM outbox_event
            """;

    private static final RowMapper<OutboxEvent> ROW_MAPPER = OutboxEventRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public OutboxEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            UUID eventId,
            AggregateType aggregateType,
            String aggregateReference,
            DomainEventType eventType,
            int eventVersion,
            String payloadJson) {
        jdbcTemplate.update(
                """
                INSERT INTO outbox_event (
                    event_id, aggregate_type, aggregate_reference, event_type, event_version, payload
                )
                VALUES (?, ?, ?, ?, ?, ?::jsonb)
                """,
                eventId,
                aggregateType.name(),
                aggregateReference,
                eventType.name(),
                eventVersion,
                payloadJson
        );
    }

    public List<OutboxEvent> findUnpublished(int limit) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + """
                 WHERE published_at IS NULL
                 ORDER BY created_at ASC, id ASC
                 LIMIT ?
                """,
                ROW_MAPPER,
                limit
        );
    }

    public void markPublished(long id, Instant publishedAt) {
        jdbcTemplate.update(
                """
                UPDATE outbox_event
                SET published_at = ?, last_error = NULL
                WHERE id = ? AND published_at IS NULL
                """,
                OffsetDateTime.ofInstant(publishedAt, java.time.ZoneOffset.UTC),
                id
        );
    }

    public void recordPublishFailure(long id, String lastError) {
        jdbcTemplate.update(
                """
                UPDATE outbox_event
                SET publish_attempts = publish_attempts + 1, last_error = ?
                WHERE id = ?
                """,
                lastError,
                id
        );
    }

    private static OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEvent(
                rs.getLong("id"),
                rs.getObject("event_id", UUID.class),
                AggregateType.valueOf(rs.getString("aggregate_type")),
                rs.getString("aggregate_reference"),
                DomainEventType.valueOf(rs.getString("event_type")),
                rs.getInt("event_version"),
                rs.getString("payload_json"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                toInstant(rs.getObject("published_at", OffsetDateTime.class)),
                rs.getInt("publish_attempts"),
                rs.getString("last_error")
        );
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
