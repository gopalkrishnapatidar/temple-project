-- NOW() is transaction-stable (same as transaction_timestamp()).
-- Use clock_timestamp() so updated_at reflects the actual UPDATE time.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = clock_timestamp();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

UPDATE application_metadata
SET value = '3'
WHERE key = 'schema_version';
