-- Add unique constraint to prevent duplicate OAuth connections for same vessel/charter, provider, and type
-- This allows upsert logic to replace existing connections with the same criteria

-- Step 1: Delete duplicate connections, keeping only the most recent one (by created_at)
DELETE FROM oauth_connections
WHERE id IN (
  SELECT id
  FROM (
    SELECT id,
           ROW_NUMBER() OVER (
             PARTITION BY COALESCE(vessel_id, '00000000-0000-0000-0000-000000000000'::uuid),
                          COALESCE(charter_id, '00000000-0000-0000-0000-000000000000'::uuid),
                          provider,
                          type
             ORDER BY created_at DESC
           ) AS row_num
    FROM oauth_connections
  ) duplicates
  WHERE row_num > 1
);

-- Step 2: Create the unique index
CREATE UNIQUE INDEX uq_oauth_connections_vessel_charter_provider_type
  ON oauth_connections(COALESCE(vessel_id, '00000000-0000-0000-0000-000000000000'::uuid),
                       COALESCE(charter_id, '00000000-0000-0000-0000-000000000000'::uuid),
                       provider,
                       type);

COMMENT ON INDEX uq_oauth_connections_vessel_charter_provider_type IS 'Ensures only one connection per vessel/charter, provider, and type combination. Uses COALESCE to handle nullable vessel_id/charter_id for unique constraint.';
