CREATE TABLE ritual (
    id                  BIGSERIAL PRIMARY KEY,
    temple_id           BIGINT NOT NULL REFERENCES temple(id) ON DELETE RESTRICT,
    type                VARCHAR(16) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    duration_minutes    INTEGER NOT NULL,
    price               NUMERIC(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ritual_type_valid
        CHECK (type IN ('PUJA', 'HAVAN')),
    CONSTRAINT ritual_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT ritual_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT ritual_price_non_negative
        CHECK (price >= 0),
    CONSTRAINT ritual_currency_supported
        CHECK (currency IN ('INR')),
    CONSTRAINT ritual_status_valid
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TRIGGER ritual_set_updated_at
    BEFORE UPDATE ON ritual
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX ritual_temple_id_type_status_idx
    ON ritual (temple_id, type, status);

CREATE TABLE ritual_slot (
    id          BIGSERIAL PRIMARY KEY,
    ritual_id   BIGINT NOT NULL REFERENCES ritual(id) ON DELETE RESTRICT,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ritual_slot_end_after_start
        CHECK (end_at > start_at),
    CONSTRAINT ritual_slot_status_valid
        CHECK (status IN ('AVAILABLE', 'CANCELLED'))
);

CREATE TRIGGER ritual_slot_set_updated_at
    BEFORE UPDATE ON ritual_slot
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX ritual_slot_ritual_id_start_at_idx
    ON ritual_slot (ritual_id, start_at, id);

UPDATE application_metadata
SET value = '7'
WHERE key = 'schema_version';
