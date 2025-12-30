-- Drop the old constraint that required exactly one of vessel_id or charter_id
ALTER TABLE oauth_connections
DROP CONSTRAINT chk_oauth_connections_level;

-- Add new constraint that requires at least one of vessel_id or charter_id
ALTER TABLE oauth_connections
ADD CONSTRAINT chk_oauth_connections_level CHECK (
  vessel_id IS NOT NULL OR charter_id IS NOT NULL
);
