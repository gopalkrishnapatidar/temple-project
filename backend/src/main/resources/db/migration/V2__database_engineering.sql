CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE application_metadata
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE application_metadata
    DROP CONSTRAINT IF EXISTS application_metadata_key_not_blank;

ALTER TABLE application_metadata
    ADD CONSTRAINT application_metadata_key_not_blank
    CHECK (length(btrim(key)) > 0);

ALTER TABLE application_metadata
    DROP CONSTRAINT IF EXISTS application_metadata_value_not_blank;

ALTER TABLE application_metadata
    ADD CONSTRAINT application_metadata_value_not_blank
    CHECK (length(btrim(value)) > 0);

DROP TRIGGER IF EXISTS application_metadata_set_updated_at ON application_metadata;

CREATE TRIGGER application_metadata_set_updated_at
    BEFORE UPDATE ON application_metadata
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

UPDATE application_metadata
SET value = '2'
WHERE key = 'schema_version';
