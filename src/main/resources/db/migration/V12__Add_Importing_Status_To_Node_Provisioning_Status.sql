-- Add 'importing' value to node_provisioning_status enum type
ALTER TYPE node_provisioning_status ADD VALUE IF NOT EXISTS 'imported';
