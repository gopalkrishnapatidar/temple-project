# Module 13 — Payments & Donations

## Architecture

- PostgreSQL is authoritative for `payment`, `donation`, and `payment_webhook_event`.
- `PaymentProvider` abstraction with local `MockPaymentProvider` (no external SDK).
- Booking payment amount is derived server-side from `ritual.price × booking.quantity` (INR only).
- Darshan bookings have no authoritative catalog price; payment initiation returns HTTP 400.
- Donation amount is client-selected but validated server-side (positive, max 2 decimal places, max 1,000,000.00 INR).
- Provider initiation runs outside the DB insert transaction; status updates use a separate transaction.

## State Models

**Payment:** `PENDING → SUCCEEDED | FAILED` (terminal states do not regress)

**Donation:** `PENDING → COMPLETED | FAILED` (synced from linked payment status)

## APIs

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/v1/bookings/{bookingReference}/payments` | JWT + `Idempotency-Key` | Ritual booking payment only |
| POST | `/api/v1/donations` | JWT + `Idempotency-Key` | Creates donation + payment |
| GET | `/api/v1/payments/{paymentReference}` | JWT | Owner / temple admin / platform admin |
| GET | `/api/v1/donations/{donationReference}` | JWT | Owner / temple admin / platform admin |
| POST | `/api/v1/payments/{paymentReference}/reconcile` | JWT | PENDING → provider terminal state |
| POST | `/api/v1/payments/webhooks/mock` | HMAC signature | Public endpoint, signature required |

## Database Constraints

- `payment_reference`, `donation_reference` UNIQUE
- `(account_id, idempotency_key)` UNIQUE on `payment` and `donation`
- `provider_reference` UNIQUE on `payment`
- `provider_event_id` UNIQUE on `payment_webhook_event`
- Partial UNIQUE on `payment(booking_id)` for active booking payments (`PENDING`/`SUCCEEDED`)
- CHECK constraints on amounts, currency (`INR`), purpose, status, and booking/donation target integrity

## Mock Provider

- Generates `mock_<uuid>` provider references.
- Deterministic outcomes from amount fractional part: `.99` → `FAILED`, `.50` → `PENDING`, otherwise `SUCCEEDED`.
- In-memory provider state supports reconciliation via `getStatus`.

## Webhook Signature

- Header: `X-Webhook-Signature: sha256=<hex>`
- HMAC-SHA256 over raw request body bytes
- Secret: `MOCK_PAYMENT_WEBHOOK_SECRET` (never hardcoded in source)

## Idempotency

- Booking/donation initiation requires `Idempotency-Key` (max 128 chars, per account).
- DB `ON CONFLICT DO NOTHING` + service replay/conflict logic (same pattern as Module 10 booking).
- Webhook idempotency via unique `provider_event_id`.

## Reconciliation

- Authenticated owner (or admin) may reconcile a `PENDING` payment against mock provider state.
- Idempotent: terminal payments and repeated calls are safe.

## Known Limitations

- No real payment gateway, refunds, or card/PCI data.
- Darshan booking payment unsupported until a catalog price exists.
- Ritual price is current configuration, not a booking-time snapshot.
- No Kafka, scheduler, or email/SMS notifications.
