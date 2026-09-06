CREATE TABLE outbox_event (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            UUID NOT NULL,
    aggregate_type      VARCHAR(32) NOT NULL,
    aggregate_reference VARCHAR(64) NOT NULL,
    event_type          VARCHAR(64) NOT NULL,
    event_version       INTEGER NOT NULL,
    payload             JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ,
    publish_attempts    INTEGER NOT NULL DEFAULT 0,
    last_error          VARCHAR(512),
    CONSTRAINT outbox_event_event_id_unique
        UNIQUE (event_id),
    CONSTRAINT outbox_event_event_version_positive
        CHECK (event_version > 0),
    CONSTRAINT outbox_event_publish_attempts_non_negative
        CHECK (publish_attempts >= 0),
    CONSTRAINT outbox_event_aggregate_type_valid
        CHECK (aggregate_type IN ('BOOKING', 'PAYMENT')),
    CONSTRAINT outbox_event_event_type_valid
        CHECK (event_type IN (
            'PAYMENT_SUCCEEDED',
            'PAYMENT_FAILED',
            'BOOKING_CONFIRMED',
            'BOOKING_CANCELLED'
        ))
);

CREATE INDEX outbox_event_unpublished_created_at_id_idx
    ON outbox_event (created_at ASC, id ASC)
    WHERE published_at IS NULL;

CREATE TABLE notification (
    id                      BIGSERIAL PRIMARY KEY,
    notification_reference  UUID NOT NULL,
    account_id              BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    channel                 VARCHAR(32) NOT NULL,
    type                    VARCHAR(64) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    message                 VARCHAR(1024) NOT NULL,
    source_event_id         UUID NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at                 TIMESTAMPTZ,
    failure_reason          VARCHAR(512),
    CONSTRAINT notification_reference_unique
        UNIQUE (notification_reference),
    CONSTRAINT notification_source_event_id_unique
        UNIQUE (source_event_id),
    CONSTRAINT notification_channel_valid
        CHECK (channel IN ('EMAIL_MOCK')),
    CONSTRAINT notification_status_valid
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT notification_type_valid
        CHECK (type IN (
            'PAYMENT_SUCCEEDED',
            'PAYMENT_FAILED',
            'BOOKING_CONFIRMED',
            'BOOKING_CANCELLED'
        ))
);

CREATE INDEX notification_account_id_created_at_id_idx
    ON notification (account_id, created_at DESC, id DESC);

UPDATE application_metadata
SET value = '10'
WHERE key = 'schema_version';
