CREATE TABLE account (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(320) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(32) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT account_email_unique UNIQUE (email),
    CONSTRAINT account_email_normalized
        CHECK (email = lower(btrim(email))),
    CONSTRAINT account_email_not_blank
        CHECK (length(btrim(email)) > 0),
    CONSTRAINT account_password_hash_not_blank
        CHECK (length(btrim(password_hash)) > 0),
    CONSTRAINT account_role_valid
        CHECK (role IN ('DEVOTEE', 'TEMPLE_ADMIN', 'PLATFORM_ADMIN')),
    CONSTRAINT account_status_valid
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TRIGGER account_set_updated_at
    BEFORE UPDATE ON account
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

UPDATE application_metadata
SET value = '4'
WHERE key = 'schema_version';
