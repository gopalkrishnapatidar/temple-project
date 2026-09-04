package com.temple.platform.payment.repository;

import com.temple.platform.payment.domain.PaymentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentWebhookEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentWebhookEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertIfAbsent(String providerEventId, String providerReference, PaymentStatus status) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO payment_webhook_event (provider_event_id, provider_reference, event_status)
                VALUES (?, ?, ?)
                ON CONFLICT (provider_event_id) DO NOTHING
                """,
                providerEventId,
                providerReference,
                status.name()
        );
        return inserted == 1;
    }

    public Optional<String> findEventStatus(String providerEventId) {
        return jdbcTemplate.query(
                "SELECT event_status FROM payment_webhook_event WHERE provider_event_id = ?",
                (rs, rowNum) -> rs.getString("event_status"),
                providerEventId
        ).stream().findFirst();
    }
}
