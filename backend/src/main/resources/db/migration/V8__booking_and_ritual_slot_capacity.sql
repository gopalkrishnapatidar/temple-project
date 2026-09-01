ALTER TABLE ritual_slot
    ADD COLUMN capacity INTEGER NOT NULL DEFAULT 1;

ALTER TABLE ritual_slot
    ADD CONSTRAINT ritual_slot_capacity_positive
        CHECK (capacity > 0);

ALTER TABLE ritual_slot
    ALTER COLUMN capacity DROP DEFAULT;

CREATE TABLE booking (
    id                  BIGSERIAL PRIMARY KEY,
    booking_reference   UUID NOT NULL,
    account_id          BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    darshan_slot_id     BIGINT REFERENCES darshan_slot(id) ON DELETE RESTRICT,
    ritual_slot_id      BIGINT REFERENCES ritual_slot(id) ON DELETE RESTRICT,
    quantity            INTEGER NOT NULL,
    status              VARCHAR(32) NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT booking_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT booking_exactly_one_slot
        CHECK (
            (darshan_slot_id IS NOT NULL AND ritual_slot_id IS NULL)
            OR (darshan_slot_id IS NULL AND ritual_slot_id IS NOT NULL)
        ),
    CONSTRAINT booking_status_valid
        CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT booking_reference_unique
        UNIQUE (booking_reference),
    CONSTRAINT booking_account_idempotency_key_unique
        UNIQUE (account_id, idempotency_key)
);

CREATE TRIGGER booking_set_updated_at
    BEFORE UPDATE ON booking
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX booking_account_id_created_at_id_idx
    ON booking (account_id, created_at DESC, id DESC);

CREATE INDEX booking_darshan_slot_confirmed_idx
    ON booking (darshan_slot_id)
    WHERE status = 'CONFIRMED' AND darshan_slot_id IS NOT NULL;

CREATE INDEX booking_ritual_slot_confirmed_idx
    ON booking (ritual_slot_id)
    WHERE status = 'CONFIRMED' AND ritual_slot_id IS NOT NULL;

UPDATE application_metadata
SET value = '8'
WHERE key = 'schema_version';
