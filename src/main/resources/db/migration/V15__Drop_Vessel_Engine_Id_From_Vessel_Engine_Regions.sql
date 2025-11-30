-- Remove vessel_engine_id from vessel_engine_regions and adjust constraints/indexes

ALTER TABLE vessel_engine_regions
  DROP CONSTRAINT IF EXISTS fk_regions_engine,
  DROP CONSTRAINT IF EXISTS uq_regions_engine_name;

DROP INDEX IF EXISTS idx_regions_engine;

ALTER TABLE vessel_engine_regions
  DROP COLUMN IF EXISTS vessel_engine_id,
  ADD CONSTRAINT uq_regions_vessel_name UNIQUE (vessel_id, name);
