package com.temple.platform.payment.repository;

import com.temple.platform.payment.domain.Payment;
import com.temple.platform.payment.domain.PaymentCurrency;
import com.temple.platform.payment.domain.PaymentPurpose;
import com.temple.platform.payment.domain.PaymentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, payment_reference, account_id, purpose, booking_id, donation_id,
                   amount, currency, status, provider_reference, idempotency_key,
                   created_at, updated_at
            FROM payment
            """;

    private static final RowMapper<Payment> ROW_MAPPER = PaymentRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Payment> insertIgnoringIdempotencyConflict(
            UUID paymentReference,
            long accountId,
            PaymentPurpose purpose,
            Long bookingId,
            Long donationId,
            BigDecimal amount,
            PaymentCurrency currency,
            PaymentStatus status,
            String providerReference,
            String idempotencyKey) {
        return jdbcTemplate.query(
                """
                INSERT INTO payment (
                    payment_reference, account_id, purpose, booking_id, donation_id,
                    amount, currency, status, provider_reference, idempotency_key
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT payment_account_idempotency_key_unique DO NOTHING
                RETURNING id, payment_reference, account_id, purpose, booking_id, donation_id,
                          amount, currency, status, provider_reference, idempotency_key,
                          created_at, updated_at
                """,
                ps -> bindInsert(
                        ps,
                        paymentReference,
                        accountId,
                        purpose,
                        bookingId,
                        donationId,
                        amount,
                        currency,
                        status,
                        providerReference,
                        idempotencyKey
                ),
                ROW_MAPPER
        ).stream().findFirst();
    }

    public void updateProviderReferenceAndStatus(long id, String providerReference, PaymentStatus status) {
        jdbcTemplate.update(
                """
                UPDATE payment
                SET provider_reference = ?, status = ?
                WHERE id = ?
                """,
                providerReference,
                status.name(),
                id
        );
    }

    public void updateStatus(long id, PaymentStatus status) {
        jdbcTemplate.update(
                "UPDATE payment SET status = ? WHERE id = ?",
                status.name(),
                id
        );
    }

    public Optional<Payment> findById(long id) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE id = ?",
                ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public Optional<Payment> findByPaymentReference(UUID paymentReference) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE payment_reference = ?",
                ROW_MAPPER,
                paymentReference
        ).stream().findFirst();
    }

    public Optional<Payment> findByAccountIdAndIdempotencyKey(long accountId, String idempotencyKey) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE account_id = ? AND idempotency_key = ?",
                ROW_MAPPER,
                accountId,
                idempotencyKey
        ).stream().findFirst();
    }

    public Optional<Payment> findByProviderReference(String providerReference) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE provider_reference = ?",
                ROW_MAPPER,
                providerReference
        ).stream().findFirst();
    }

    public Optional<UUID> findPaymentReferenceByDonationId(long donationId) {
        return jdbcTemplate.query(
                """
                SELECT payment_reference
                FROM payment
                WHERE donation_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("payment_reference", UUID.class),
                donationId
        ).stream().findFirst();
    }

    public Optional<Payment> findActiveByBookingId(long bookingId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + """
                 WHERE booking_id = ?
                   AND purpose = 'BOOKING'
                   AND status IN ('PENDING', 'SUCCEEDED')
                 ORDER BY id DESC
                 LIMIT 1
                """,
                ROW_MAPPER,
                bookingId
        ).stream().findFirst();
    }

    public Optional<Long> findTempleIdByPaymentId(long paymentId) {
        List<Long> templeIds = jdbcTemplate.query(
                """
                SELECT CASE
                    WHEN p.donation_id IS NOT NULL THEN d.temple_id
                    WHEN b.ritual_slot_id IS NOT NULL THEN r.temple_id
                    WHEN b.darshan_slot_id IS NOT NULL THEN dar.temple_id
                END AS temple_id
                FROM payment p
                LEFT JOIN donation d ON d.id = p.donation_id
                LEFT JOIN booking b ON b.id = p.booking_id
                LEFT JOIN ritual_slot rs ON rs.id = b.ritual_slot_id
                LEFT JOIN ritual r ON r.id = rs.ritual_id
                LEFT JOIN darshan_slot ds ON ds.id = b.darshan_slot_id
                LEFT JOIN darshan dar ON dar.id = ds.darshan_id
                WHERE p.id = ?
                """,
                (rs, rowNum) -> {
                    long templeId = rs.getLong("temple_id");
                    return rs.wasNull() ? null : templeId;
                },
                paymentId
        );
        return templeIds.stream().filter(id -> id != null).findFirst();
    }

    private static void bindInsert(
            java.sql.PreparedStatement ps,
            UUID paymentReference,
            long accountId,
            PaymentPurpose purpose,
            Long bookingId,
            Long donationId,
            BigDecimal amount,
            PaymentCurrency currency,
            PaymentStatus status,
            String providerReference,
            String idempotencyKey) throws SQLException {
        ps.setObject(1, paymentReference);
        ps.setLong(2, accountId);
        ps.setString(3, purpose.name());
        if (bookingId == null) {
            ps.setNull(4, Types.BIGINT);
        } else {
            ps.setLong(4, bookingId);
        }
        if (donationId == null) {
            ps.setNull(5, Types.BIGINT);
        } else {
            ps.setLong(5, donationId);
        }
        ps.setBigDecimal(6, amount);
        ps.setString(7, currency.name());
        ps.setString(8, status.name());
        if (providerReference == null) {
            ps.setNull(9, Types.VARCHAR);
        } else {
            ps.setString(9, providerReference);
        }
        ps.setString(10, idempotencyKey);
    }

    private static Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
        long bookingId = rs.getLong("booking_id");
        Long bookingIdValue = rs.wasNull() ? null : bookingId;
        long donationId = rs.getLong("donation_id");
        Long donationIdValue = rs.wasNull() ? null : donationId;
        String providerReference = rs.getString("provider_reference");
        return new Payment(
                rs.getLong("id"),
                rs.getObject("payment_reference", UUID.class),
                rs.getLong("account_id"),
                PaymentPurpose.valueOf(rs.getString("purpose")),
                bookingIdValue,
                donationIdValue,
                rs.getBigDecimal("amount"),
                PaymentCurrency.valueOf(rs.getString("currency")),
                PaymentStatus.valueOf(rs.getString("status")),
                providerReference,
                rs.getString("idempotency_key"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class))
        );
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
