-- Split the legacy `location` column into `location_city` and `location_country`
-- `location_city` remains optional; `location_country` is an optional ISO 2-char code

ALTER TABLE vessel_engine_regions
  ADD COLUMN IF NOT EXISTS location_city text,
  ADD COLUMN IF NOT EXISTS location_country char(2);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'vessel_engine_regions'
      AND column_name = 'location'
  ) THEN
    UPDATE vessel_engine_regions
    SET location_city = COALESCE(location_city, location)
    WHERE location IS NOT NULL
      AND location_city IS NULL;
  END IF;
END $$;

ALTER TABLE vessel_engine_regions
  DROP COLUMN IF EXISTS location;
