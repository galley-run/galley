-- Allow null on deploy_mode for controller nodes
-- Controller nodes don't have a deploy mode, only worker and controller_worker nodes do

ALTER TABLE vessel_engine_nodes
ALTER COLUMN deploy_mode DROP NOT NULL;
