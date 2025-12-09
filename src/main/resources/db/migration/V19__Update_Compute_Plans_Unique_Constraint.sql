-- Update unique constraint on charter_compute_plans to include application

-- Drop the existing unique constraint on (charter_id, name)
DROP INDEX IF EXISTS uq_compute_plans_charter_name;

-- Create new unique constraint on (charter_id, application, name)
CREATE UNIQUE INDEX IF NOT EXISTS uq_compute_plans_charter_application_name
  ON charter_compute_plans(charter_id, application, name)
  WHERE deleted_at IS NULL;
