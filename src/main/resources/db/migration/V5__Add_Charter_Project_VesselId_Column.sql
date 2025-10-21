ALTER TABLE charter_projects
  ADD COLUMN vessel_id UUID;

-- Add foreign key index
CREATE INDEX IF NOT EXISTS idx_charter_projects_vessel_id ON charter_projects (vessel_id);
CREATE INDEX IF NOT EXISTS idx_projects_charter_vessel_deleted ON charter_projects(charter_id, vessel_id, deleted_at);

-- Add foreign key constraint
ALTER TABLE charter_projects
  ADD CONSTRAINT fk_charter_projects_vessel
    FOREIGN KEY (vessel_id) REFERENCES vessels (id);
