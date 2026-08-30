CREATE TABLE temple (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100),
    country     VARCHAR(100) NOT NULL,
    timezone    VARCHAR(64) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT temple_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT temple_city_not_blank
        CHECK (length(btrim(city)) > 0),
    CONSTRAINT temple_country_not_blank
        CHECK (length(btrim(country)) > 0),
    CONSTRAINT temple_timezone_not_blank
        CHECK (length(btrim(timezone)) > 0),
    CONSTRAINT temple_status_valid
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TRIGGER temple_set_updated_at
    BEFORE UPDATE ON temple
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE temple_admin_assignment (
    account_id  BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    temple_id   BIGINT NOT NULL REFERENCES temple(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_id, temple_id)
);

CREATE INDEX temple_admin_assignment_temple_id_idx
    ON temple_admin_assignment (temple_id);

CREATE TABLE temple_event (
    id          BIGSERIAL PRIMARY KEY,
    temple_id   BIGINT NOT NULL REFERENCES temple(id) ON DELETE RESTRICT,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT temple_event_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT temple_event_status_valid
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT temple_event_end_after_start
        CHECK (end_at > start_at)
);

CREATE TRIGGER temple_event_set_updated_at
    BEFORE UPDATE ON temple_event
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE INDEX temple_event_temple_id_start_at_idx
    ON temple_event (temple_id, start_at, id);

CREATE INDEX temple_status_idx
    ON temple (status);

UPDATE application_metadata
SET value = '5'
WHERE key = 'schema_version';
