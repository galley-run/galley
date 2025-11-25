-- Add provisioning status enum type
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'node_provisioning_status') THEN
    CREATE TYPE node_provisioning_status AS ENUM ('open','ready');
  END IF;
END $$;

-- Add provisioning_status and os_metadata columns to vessel_engine_nodes
ALTER TABLE vessel_engine_nodes
  ADD COLUMN IF NOT EXISTS provisioning_status node_provisioning_status NOT NULL DEFAULT 'open',
  ADD COLUMN IF NOT EXISTS os_metadata jsonb;

-- Create index on provisioning_status for queries filtering by status
CREATE INDEX IF NOT EXISTS idx_nodes_provisioning_status ON vessel_engine_nodes(provisioning_status);

-- Create GIN index on os_metadata for JSONB queries
CREATE INDEX IF NOT EXISTS idx_nodes_os_metadata_gin ON vessel_engine_nodes USING gin (os_metadata jsonb_path_ops);
