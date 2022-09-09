UPDATE api_key
set key_hash    = encode(sha256(key::bytea), 'base64'),
    description = LEFT(key, 5) || '......' || RIGHT(key, 5),
    key = null
where key_hash is null and key is not null;

CREATE OR REPLACE FUNCTION hash_new_api_key()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.key is not null and NEW.key_hash is null THEN
        NEW.key_hash = encode(sha256(NEW.key::bytea), 'base64');
        NEW.key = null;
    END IF;
    RETURN NEW;
END
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS hash_new_api_key ON api_key;

CREATE TRIGGER hash_new_api_key
    BEFORE INSERT OR
        UPDATE
    ON api_key
    FOR EACH ROW
EXECUTE PROCEDURE hash_new_api_key();
