-- Add agent_connection_status enum type
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'agent_connection_status') THEN
    CREATE TYPE agent_connection_status AS ENUM ('connected', 'disconnected', 'error');
  END IF;
END $$;

-- Add columns to vessel_engines table
ALTER TABLE vessel_engines
  ADD COLUMN IF NOT EXISTS agent_connection_status agent_connection_status NOT NULL DEFAULT 'disconnected',
  ADD COLUMN IF NOT EXISTS last_connection_error text;

-- Add index for querying by connection status
CREATE INDEX IF NOT EXISTS idx_vessel_engines_connection_status
  ON vessel_engines(agent_connection_status);
