-- Add last_connection_at timestamp to vessel_engines table
ALTER TABLE vessel_engines
  ADD COLUMN IF NOT EXISTS last_agent_connection_at timestamp with time zone;

-- Create index for querying by last connection timestamp
CREATE INDEX IF NOT EXISTS idx_vessel_engines_last_connection_at
  ON vessel_engines(last_agent_connection_at);
