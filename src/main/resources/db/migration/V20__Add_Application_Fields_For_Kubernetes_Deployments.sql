-- Add new columns to project_applications for Kubernetes deployment configuration
ALTER TABLE project_applications
    ADD COLUMN charter_compute_plan_id UUID,
    ADD COLUMN description TEXT,
    ADD COLUMN labels JSONB,
    ADD COLUMN annotations JSONB,
    ADD COLUMN deployment JSONB,
    ADD COLUMN pod JSONB;

-- Add foreign key constraint to compute_plans
ALTER TABLE project_applications
    ADD CONSTRAINT fk_applications_charter_compute_plan
        FOREIGN KEY (charter_compute_plan_id)
            REFERENCES charter_compute_plans(id)
            ON DELETE SET NULL;

-- Add index on compute_plan_id for efficient lookups
CREATE INDEX idx_applications_charter_compute_plan_id ON project_applications(charter_compute_plan_id);
