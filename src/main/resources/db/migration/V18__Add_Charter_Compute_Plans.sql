-- Add charter_compute_plans table

CREATE TABLE IF NOT EXISTS charter_compute_plans (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  name text NOT NULL,
  application text,
  requests_cpu text NOT NULL,
  requests_memory text NOT NULL,
  limits_cpu text,
  limits_memory text,
  billing_enabled boolean NOT NULL DEFAULT false,
  billing_period text,
  billing_unit_price text,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_compute_plans_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_compute_plans_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_compute_plans_vessel ON charter_compute_plans(vessel_id);
CREATE INDEX IF NOT EXISTS idx_compute_plans_charter ON charter_compute_plans(charter_id);
CREATE INDEX IF NOT EXISTS idx_compute_plans_deleted ON charter_compute_plans(deleted_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_compute_plans_charter_name ON charter_compute_plans(charter_id, name) WHERE deleted_at IS NULL;

-- Enable RLS for charter_compute_plans
ALTER TABLE charter_compute_plans ENABLE ROW LEVEL SECURITY;

-- RLS policies for charter_compute_plans
CREATE POLICY p_compute_plans_select ON charter_compute_plans
  FOR SELECT USING (galley_has_charter_access(charter_id));
CREATE POLICY p_compute_plans_insert ON charter_compute_plans
  FOR INSERT WITH CHECK (galley_has_charter_access(charter_id));
CREATE POLICY p_compute_plans_update ON charter_compute_plans
  FOR UPDATE USING (galley_has_charter_access(charter_id));
CREATE POLICY p_compute_plans_delete ON charter_compute_plans
  FOR DELETE USING (galley_has_charter_access(charter_id));

-- Add outbox trigger
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgname = 'trg_outbox_charter_compute_plans'
  ) THEN
    CREATE TRIGGER trg_outbox_charter_compute_plans
    AFTER INSERT OR UPDATE OR DELETE ON charter_compute_plans
    FOR EACH ROW
    EXECUTE FUNCTION galley_outbox_trigger();
  END IF;
END $$;
