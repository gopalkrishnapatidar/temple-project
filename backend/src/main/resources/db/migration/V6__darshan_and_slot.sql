CREATE TABLE darshan (
    id          BIGSERIAL PRIMARY KEY,
    temple_id   BIGINT NOT NULL REFERENCES temple(id) ON DELETE RESTRICT,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT darshan_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT darshan_status_valid
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TRIGGER darshan_set_updated_at
    BEFORE UPDATE ON darshan
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX darshan_temple_id_idx
    ON darshan (temple_id);

CREATE INDEX darshan_temple_id_status_idx
    ON darshan (temple_id, status);

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE darshan_slot (
    id          BIGSERIAL PRIMARY KEY,
    darshan_id  BIGINT NOT NULL REFERENCES darshan(id) ON DELETE RESTRICT,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    capacity    INTEGER NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT darshan_slot_capacity_positive
        CHECK (capacity > 0),
    CONSTRAINT darshan_slot_end_after_start
        CHECK (end_at > start_at),
    CONSTRAINT darshan_slot_status_valid
        CHECK (status IN ('AVAILABLE', 'CANCELLED')),
    CONSTRAINT darshan_slot_no_time_overlap
        EXCLUDE USING gist (
            darshan_id WITH =,
            tstzrange(start_at, end_at, '[)') WITH &&
        ) WHERE (status = 'AVAILABLE')
);

CREATE TRIGGER darshan_slot_set_updated_at
    BEFORE UPDATE ON darshan_slot
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX darshan_slot_darshan_id_start_at_idx
    ON darshan_slot (darshan_id, start_at, id);

CREATE INDEX darshan_slot_darshan_id_start_at_end_at_idx
    ON darshan_slot (darshan_id, start_at, end_at)
    WHERE status = 'AVAILABLE';

UPDATE application_metadata
SET value = '6'
WHERE key = 'schema_version';
