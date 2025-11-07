-- Split location column into location_city and location_country in vessel_engine_regions table

-- Add new columns
ALTER TABLE vessel_engine_regions
  ADD COLUMN location_city text,
  ADD COLUMN location_country char(2);

-- Drop the old location column
ALTER TABLE vessel_engine_regions
  DROP COLUMN location;
