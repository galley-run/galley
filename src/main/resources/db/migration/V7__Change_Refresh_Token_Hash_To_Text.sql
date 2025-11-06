-- Change refresh_token_hash from bytea to text
ALTER TABLE sessions
  ALTER COLUMN refresh_token_hash TYPE text
  USING encode(refresh_token_hash, 'hex');
