package com.temple.platform.booking.repository;

import com.temple.platform.booking.domain.Booking;
import com.temple.platform.booking.domain.BookingStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BookingRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, booking_reference, account_id, darshan_slot_id, ritual_slot_id,
                   quantity, status, idempotency_key, created_at, updated_at
            FROM booking
            """;

    private static final RowMapper<Booking> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Booking> insertIgnoringIdempotencyConflict(
            UUID bookingReference,
            long accountId,
            Long darshanSlotId,
            Long ritualSlotId,
            int quantity,
            BookingStatus status,
            String idempotencyKey) {
        return jdbcTemplate.query(
                """
                INSERT INTO booking (
                    booking_reference, account_id, darshan_slot_id, ritual_slot_id,
                    quantity, status, idempotency_key
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT booking_account_idempotency_key_unique DO NOTHING
                RETURNING id, booking_reference, account_id, darshan_slot_id, ritual_slot_id,
                          quantity, status, idempotency_key, created_at, updated_at
                """,
                ps -> {
                    ps.setObject(1, bookingReference);
                    ps.setLong(2, accountId);
                    if (darshanSlotId == null) {
                        ps.setNull(3, Types.BIGINT);
                    } else {
                        ps.setLong(3, darshanSlotId);
                    }
                    if (ritualSlotId == null) {
                        ps.setNull(4, Types.BIGINT);
                    } else {
                        ps.setLong(4, ritualSlotId);
                    }
                    ps.setInt(5, quantity);
                    ps.setString(6, status.name());
                    ps.setString(7, idempotencyKey);
                },
                ROW_MAPPER
        ).stream().findFirst();
    }

    public Optional<Booking> findById(long id) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE id = ?",
                ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public Optional<Booking> findByBookingReference(UUID bookingReference) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE booking_reference = ?",
                ROW_MAPPER,
                bookingReference
        ).stream().findFirst();
    }

    public Optional<Booking> findByAccountIdAndIdempotencyKey(long accountId, String idempotencyKey) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE account_id = ? AND idempotency_key = ?",
                ROW_MAPPER,
                accountId,
                idempotencyKey
        ).stream().findFirst();
    }

    public int sumConfirmedQuantityForDarshanSlot(long darshanSlotId) {
        Integer sum = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(quantity), 0)
                FROM booking
                WHERE darshan_slot_id = ? AND status = 'CONFIRMED'
                """,
                Integer.class,
                darshanSlotId
        );
        return sum == null ? 0 : sum;
    }

    public int sumConfirmedQuantityForRitualSlot(long ritualSlotId) {
        Integer sum = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(quantity), 0)
                FROM booking
                WHERE ritual_slot_id = ? AND status = 'CONFIRMED'
                """,
                Integer.class,
                ritualSlotId
        );
        return sum == null ? 0 : sum;
    }

    public Optional<Long> findTempleIdByBookingId(long bookingId) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(d.temple_id, r.temple_id) AS temple_id
                FROM booking b
                LEFT JOIN darshan_slot ds ON b.darshan_slot_id = ds.id
                LEFT JOIN darshan d ON ds.darshan_id = d.id
                LEFT JOIN ritual_slot rs ON b.ritual_slot_id = rs.id
                LEFT JOIN ritual r ON rs.ritual_id = r.id
                WHERE b.id = ?
                """,
                (rs, rowNum) -> rs.getObject("temple_id", Long.class),
                bookingId
        ).stream().findFirst();
    }

    public List<Booking> findByAccountId(long accountId, int limit, int offset) {
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
                "SELECT COUNT(*) FROM booking WHERE account_id = ?",
                Long.class,
                accountId
        );
        return count == null ? 0 : count;
    }

    public List<Booking> findAll(int limit, int offset) {
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
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM booking", Long.class);
        return count == null ? 0 : count;
    }

    public List<Booking> findForTempleAdmin(long templeAdminAccountId, int limit, int offset) {
        return jdbcTemplate.query(
                """
                SELECT b.id, b.booking_reference, b.account_id, b.darshan_slot_id, b.ritual_slot_id,
                       b.quantity, b.status, b.idempotency_key, b.created_at, b.updated_at
                FROM booking b
                LEFT JOIN darshan_slot ds ON b.darshan_slot_id = ds.id
                LEFT JOIN darshan d ON ds.darshan_id = d.id
                LEFT JOIN ritual_slot rs ON b.ritual_slot_id = rs.id
                LEFT JOIN ritual r ON rs.ritual_id = r.id
                WHERE d.temple_id IN (
                        SELECT temple_id FROM temple_admin_assignment WHERE account_id = ?
                    )
                   OR r.temple_id IN (
                        SELECT temple_id FROM temple_admin_assignment WHERE account_id = ?
                    )
                ORDER BY b.created_at DESC, b.id DESC
                LIMIT ? OFFSET ?
                """,
                ROW_MAPPER,
                templeAdminAccountId,
                templeAdminAccountId,
                limit,
                offset
        );
    }

    public long countForTempleAdmin(long templeAdminAccountId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM booking b
                LEFT JOIN darshan_slot ds ON b.darshan_slot_id = ds.id
                LEFT JOIN darshan d ON ds.darshan_id = d.id
                LEFT JOIN ritual_slot rs ON b.ritual_slot_id = rs.id
                LEFT JOIN ritual r ON rs.ritual_id = r.id
                WHERE d.temple_id IN (
                        SELECT temple_id FROM temple_admin_assignment WHERE account_id = ?
                    )
                   OR r.temple_id IN (
                        SELECT temple_id FROM temple_admin_assignment WHERE account_id = ?
                    )
                """,
                Long.class,
                templeAdminAccountId,
                templeAdminAccountId
        );
        return count == null ? 0 : count;
    }

    public boolean updateStatus(long id, BookingStatus status) {
        return jdbcTemplate.update(
                "UPDATE booking SET status = ? WHERE id = ?",
                status.name(),
                id
        ) == 1;
    }

    private static Booking mapRow(ResultSet rs) throws SQLException {
        Long darshanSlotId = rs.getObject("darshan_slot_id", Long.class);
        Long ritualSlotId = rs.getObject("ritual_slot_id", Long.class);
        return new Booking(
                rs.getLong("id"),
                rs.getObject("booking_reference", UUID.class),
                rs.getLong("account_id"),
                darshanSlotId,
                ritualSlotId,
                rs.getInt("quantity"),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getString("idempotency_key"),
                toInstant(rs, "created_at"),
                toInstant(rs, "updated_at")
        );
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
