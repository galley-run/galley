-- Drop the existing unique constraint on ip_address
DROP INDEX IF EXISTS uq_nodes_ip;

-- Create a new composite unique constraint on (vessel_engine_id, ip_address)
CREATE UNIQUE INDEX IF NOT EXISTS uq_nodes_engine_ip ON vessel_engine_nodes(vessel_engine_id, ip_address);
