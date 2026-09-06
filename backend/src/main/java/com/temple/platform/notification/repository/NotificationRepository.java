package com.temple.platform.notification.repository;

import com.temple.platform.notification.domain.DomainEventType;
import com.temple.platform.notification.domain.Notification;
import com.temple.platform.notification.domain.NotificationChannel;
import com.temple.platform.notification.domain.NotificationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, notification_reference, account_id, channel, type, status,
                   title, message, source_event_id, created_at, sent_at, failure_reason
            FROM notification
            """;

    private static final RowMapper<Notification> ROW_MAPPER = NotificationRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Notification> insertIfAbsent(
            UUID notificationReference,
            long accountId,
            NotificationChannel channel,
            DomainEventType type,
            String title,
            String message,
            UUID sourceEventId) {
        return jdbcTemplate.query(
                """
                INSERT INTO notification (
                    notification_reference, account_id, channel, type, status,
                    title, message, source_event_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT notification_source_event_id_unique DO NOTHING
                RETURNING id, notification_reference, account_id, channel, type, status,
                          title, message, source_event_id, created_at, sent_at, failure_reason
                """,
                ps -> {
                    ps.setObject(1, notificationReference);
                    ps.setLong(2, accountId);
                    ps.setString(3, channel.name());
                    ps.setString(4, type.name());
                    ps.setString(5, NotificationStatus.PENDING.name());
                    ps.setString(6, title);
                    ps.setString(7, message);
                    ps.setObject(8, sourceEventId);
                },
                ROW_MAPPER
        ).stream().findFirst();
    }

    public void markSent(long id, Instant sentAt) {
        jdbcTemplate.update(
                """
                UPDATE notification
                SET status = ?, sent_at = ?, failure_reason = NULL
                WHERE id = ?
                """,
                NotificationStatus.SENT.name(),
                OffsetDateTime.ofInstant(sentAt, java.time.ZoneOffset.UTC),
                id
        );
    }

    public void markFailed(long id, String failureReason) {
        jdbcTemplate.update(
                """
                UPDATE notification
                SET status = ?, failure_reason = ?
                WHERE id = ?
                """,
                NotificationStatus.FAILED.name(),
                failureReason,
                id
        );
    }

    public Optional<Notification> findBySourceEventId(UUID sourceEventId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE source_event_id = ?",
                ROW_MAPPER,
                sourceEventId
        ).stream().findFirst();
    }

    public Optional<Notification> findByNotificationReference(UUID notificationReference) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE notification_reference = ?",
                ROW_MAPPER,
                notificationReference
        ).stream().findFirst();
    }

    public List<Notification> findByAccountId(long accountId, int limit, int offset) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + """
                 WHERE account_id = ?
                 ORDER BY created_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """,
                ROW_MAPPER,
                accountId,
                limit,
                offset
        );
    }

    public long countByAccountId(long accountId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE account_id = ?",
                Long.class,
                accountId
        );
        return count == null ? 0L : count;
    }

    public List<Notification> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + """
                 ORDER BY created_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """,
                ROW_MAPPER,
                limit,
                offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification", Long.class);
        return count == null ? 0L : count;
    }

    private static Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Notification(
                rs.getLong("id"),
                rs.getObject("notification_reference", UUID.class),
                rs.getLong("account_id"),
                NotificationChannel.valueOf(rs.getString("channel")),
                DomainEventType.valueOf(rs.getString("type")),
                NotificationStatus.valueOf(rs.getString("status")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getObject("source_event_id", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                toInstant(rs.getObject("sent_at", OffsetDateTime.class)),
                rs.getString("failure_reason")
        );
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
