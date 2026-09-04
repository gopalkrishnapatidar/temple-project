CREATE TABLE donation (
    id                  BIGSERIAL PRIMARY KEY,
    donation_reference  UUID NOT NULL,
    temple_id           BIGINT NOT NULL REFERENCES temple(id) ON DELETE RESTRICT,
    account_id          BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    amount              NUMERIC(12, 2) NOT NULL,
    currency            VARCHAR(8) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT donation_reference_unique
        UNIQUE (donation_reference),
    CONSTRAINT donation_account_idempotency_key_unique
        UNIQUE (account_id, idempotency_key),
    CONSTRAINT donation_amount_positive
        CHECK (amount > 0),
    CONSTRAINT donation_currency_valid
        CHECK (currency IN ('INR')),
    CONSTRAINT donation_status_valid
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TRIGGER donation_set_updated_at
    BEFORE UPDATE ON donation
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX donation_account_id_created_at_id_idx
    ON donation (account_id, created_at DESC, id DESC);

CREATE TABLE payment (
    id                  BIGSERIAL PRIMARY KEY,
    payment_reference   UUID NOT NULL,
    account_id          BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    purpose             VARCHAR(32) NOT NULL,
    booking_id          BIGINT REFERENCES booking(id) ON DELETE RESTRICT,
    donation_id         BIGINT REFERENCES donation(id) ON DELETE RESTRICT,
    amount              NUMERIC(12, 2) NOT NULL,
    currency            VARCHAR(8) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    provider_reference  VARCHAR(128),
    idempotency_key     VARCHAR(128) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT payment_reference_unique
        UNIQUE (payment_reference),
    CONSTRAINT payment_account_idempotency_key_unique
        UNIQUE (account_id, idempotency_key),
    CONSTRAINT payment_provider_reference_unique
        UNIQUE (provider_reference),
    CONSTRAINT payment_amount_non_negative
        CHECK (amount >= 0),
    CONSTRAINT payment_currency_valid
        CHECK (currency IN ('INR')),
    CONSTRAINT payment_status_valid
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT payment_purpose_valid
        CHECK (purpose IN ('BOOKING', 'DONATION')),
    CONSTRAINT payment_exactly_one_target
        CHECK (
            (purpose = 'BOOKING' AND booking_id IS NOT NULL AND donation_id IS NULL)
            OR (purpose = 'DONATION' AND donation_id IS NOT NULL AND booking_id IS NULL)
        )
);

CREATE UNIQUE INDEX payment_booking_active_unique
    ON payment (booking_id)
    WHERE purpose = 'BOOKING' AND status IN ('PENDING', 'SUCCEEDED');

CREATE TRIGGER payment_set_updated_at
    BEFORE UPDATE ON payment
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX payment_account_id_created_at_id_idx
    ON payment (account_id, created_at DESC, id DESC);

CREATE TABLE payment_webhook_event (
    id                  BIGSERIAL PRIMARY KEY,
    provider_event_id   VARCHAR(128) NOT NULL,
    provider_reference  VARCHAR(128) NOT NULL,
    event_status        VARCHAR(32) NOT NULL,
    processed_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT payment_webhook_event_provider_event_id_unique
        UNIQUE (provider_event_id),
    CONSTRAINT payment_webhook_event_status_valid
        CHECK (event_status IN ('SUCCEEDED', 'FAILED'))
);

UPDATE application_metadata
SET value = '9'
WHERE key = 'schema_version';
