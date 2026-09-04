package com.temple.platform.donation.repository;

import com.temple.platform.donation.domain.Donation;
import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.payment.domain.PaymentCurrency;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DonationRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, donation_reference, temple_id, account_id, amount, currency,
                   status, idempotency_key, created_at, updated_at
            FROM donation
            """;

    private static final RowMapper<Donation> ROW_MAPPER = DonationRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public DonationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Donation> insertIgnoringIdempotencyConflict(
            UUID donationReference,
            long templeId,
            long accountId,
            BigDecimal amount,
            PaymentCurrency currency,
            DonationStatus status,
            String idempotencyKey) {
        return jdbcTemplate.query(
                """
                INSERT INTO donation (
                    donation_reference, temple_id, account_id, amount, currency, status, idempotency_key
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT donation_account_idempotency_key_unique DO NOTHING
                RETURNING id, donation_reference, temple_id, account_id, amount, currency,
                          status, idempotency_key, created_at, updated_at
                """,
                ps -> {
                    ps.setObject(1, donationReference);
                    ps.setLong(2, templeId);
                    ps.setLong(3, accountId);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, currency.name());
                    ps.setString(6, status.name());
                    ps.setString(7, idempotencyKey);
                },
                ROW_MAPPER
        ).stream().findFirst();
    }

    public void updateStatus(long id, DonationStatus status) {
        jdbcTemplate.update(
                "UPDATE donation SET status = ? WHERE id = ?",
                status.name(),
                id
        );
    }

    public Optional<Donation> findById(long id) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE id = ?",
                ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public Optional<Donation> findByDonationReference(UUID donationReference) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE donation_reference = ?",
                ROW_MAPPER,
                donationReference
        ).stream().findFirst();
    }

    public Optional<Donation> findByAccountIdAndIdempotencyKey(long accountId, String idempotencyKey) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE account_id = ? AND idempotency_key = ?",
                ROW_MAPPER,
                accountId,
                idempotencyKey
        ).stream().findFirst();
    }

    private static Donation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Donation(
                rs.getLong("id"),
                rs.getObject("donation_reference", UUID.class),
                rs.getLong("temple_id"),
                rs.getLong("account_id"),
                rs.getBigDecimal("amount"),
                PaymentCurrency.valueOf(rs.getString("currency")),
                DonationStatus.valueOf(rs.getString("status")),
                rs.getString("idempotency_key"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class))
        );
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
