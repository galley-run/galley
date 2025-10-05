
-- Galley Database Schema (PostgreSQL)
-- Generated on 2025-10-03

-- ==============================
-- Extensions (safe if already present)
-- ==============================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ==============================
-- Enum Types
-- ==============================
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'charter_role') THEN
    CREATE TYPE charter_role AS ENUM ('captain','boatswain','deckhand','steward','purser');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
    CREATE TYPE member_status AS ENUM ('invited','active');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'invite_status') THEN
    CREATE TYPE invite_status AS ENUM ('pending','accepted','declined','expired','revoked');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'api_key_status') THEN
    CREATE TYPE api_key_status AS ENUM ('active','revoked');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vessel_role') THEN
    CREATE TYPE vessel_role AS ENUM ('captain','member');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sign_up_intent') THEN
    CREATE TYPE sign_up_intent AS ENUM ('exploring','business','private');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'technical_experience') THEN
    CREATE TYPE technical_experience AS ENUM ('non_tech','junior_dev','experienced','tech_leadership');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'api_gateway_engine') THEN
    CREATE TYPE api_gateway_engine AS ENUM ('traefik');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'engine_mode') THEN
    CREATE TYPE engine_mode AS ENUM ('managed_cloud','managed_engine','controlled_engine');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'node_type') THEN
    CREATE TYPE node_type AS ENUM ('controller','worker','controller_worker');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'node_deploy_mode') THEN
    CREATE TYPE node_deploy_mode AS ENUM ('applications_databases','applications','databases');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'geo_region') THEN
    CREATE TYPE geo_region AS ENUM ('eu','usa','apac','latam','africa','na');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'outbox_status') THEN
    CREATE TYPE outbox_status AS ENUM ('pending','processing','delivered','failed','dead');
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'delivery_status') THEN
    CREATE TYPE delivery_status AS ENUM ('queued','sending','ok','failed','dead');
  END IF;
END $$;

-- ==============================
-- Tables (ordered by FK dependencies)
-- ==============================

-- users
CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  email citext NOT NULL UNIQUE,
  first_name text NOT NULL,
  last_name text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- vessels
CREATE TABLE IF NOT EXISTS vessels (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  name text NOT NULL,
  user_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_vessels_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_vessels_user ON vessels(user_id);

-- charters
CREATE TABLE IF NOT EXISTS charters (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid,
  user_id uuid NOT NULL,
  name text NOT NULL,
  description text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_charters_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE NO ACTION,
  CONSTRAINT fk_charters_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_charters_vessel ON charters(vessel_id);
CREATE INDEX IF NOT EXISTS idx_charters_vessel_created ON charters(vessel_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_charters_vessel_name ON charters(vessel_id, name);
CREATE INDEX IF NOT EXISTS idx_charters_deleted_at ON charters(deleted_at);

-- charter_projects
CREATE TABLE IF NOT EXISTS charter_projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  charter_id uuid,
  name text NOT NULL,
  environment text NOT NULL DEFAULT 'production',
  purpose text,
  deleted_at timestamptz,
  CONSTRAINT fk_projects_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_projects_charter ON charter_projects(charter_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_projects_name_env ON charter_projects(charter_id, name, environment);
CREATE INDEX IF NOT EXISTS idx_projects_charter_deleted ON charter_projects(charter_id, deleted_at);

-- user_identities
CREATE TABLE IF NOT EXISTS user_identities (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL,
  provider text NOT NULL,
  subject text NOT NULL,
  email citext,
  raw_profile jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_user_identities_provider_subject UNIQUE (provider, subject)
);
CREATE INDEX IF NOT EXISTS idx_user_identities_user_provider ON user_identities(user_id, provider);
CREATE INDEX IF NOT EXISTS idx_user_identities_provider_subject ON user_identities(provider, subject);
CREATE INDEX IF NOT EXISTS idx_user_identities_email ON user_identities(email);

-- webauthn_credentials
CREATE TABLE IF NOT EXISTS webauthn_credentials (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL,
  credential_id bytea NOT NULL UNIQUE,
  public_key bytea NOT NULL,
  aaguid uuid,
  sign_count bigint NOT NULL DEFAULT 0,
  transports text[],
  is_discoverable boolean,
  is_backup_eligible boolean,
  name text,
  last_used_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_webauthn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_webauthn_user ON webauthn_credentials(user_id);

-- api_keys
CREATE TABLE IF NOT EXISTS api_keys (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL,
  name text NOT NULL,
  prefix text NOT NULL UNIQUE,
  token_hash bytea NOT NULL,
  status api_key_status NOT NULL DEFAULT 'active',
  last_used_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_status ON api_keys(user_id, status);
CREATE INDEX IF NOT EXISTS idx_api_keys_last_used ON api_keys(last_used_at);

-- email_login_tokens
CREATE TABLE IF NOT EXISTS email_login_tokens (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid,
  email citext NOT NULL,
  token_hash bytea NOT NULL UNIQUE,
  purpose text NOT NULL DEFAULT 'magic_link',
  ip inet,
  user_agent text,
  expires_at timestamptz NOT NULL,
  used_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_email_login_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_email ON email_login_tokens(email);
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_expires ON email_login_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_user ON email_login_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_lookup ON email_login_tokens(email, purpose, expires_at);
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_used ON email_login_tokens(used_at);
-- Partial for active tokens
CREATE INDEX IF NOT EXISTS idx_email_login_tokens_active ON email_login_tokens(email, purpose)
WHERE used_at IS NULL;

-- crew
CREATE TABLE IF NOT EXISTS crew (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id uuid NOT NULL,
  vessel_id uuid NOT NULL,
  vessel_role vessel_role NOT NULL DEFAULT 'captain',
  status member_status NOT NULL DEFAULT 'invited',
  created_at timestamptz NOT NULL DEFAULT now(),
  activated_at timestamptz,
  deleted_at timestamptz,
  CONSTRAINT fk_crew_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_crew_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT uq_crew_user_vessel UNIQUE (user_id, vessel_id)
);
CREATE INDEX IF NOT EXISTS idx_crew_vessel ON crew(vessel_id);
CREATE INDEX IF NOT EXISTS idx_crew_user ON crew(user_id);
CREATE INDEX IF NOT EXISTS idx_crew_status ON crew(status);
-- Partial active crew per vessel
CREATE INDEX IF NOT EXISTS idx_crew_active ON crew(vessel_id)
WHERE deleted_at IS NULL AND status = 'active';

-- crew_charter_member
CREATE TABLE IF NOT EXISTS crew_charter_member (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  crew_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  charter_role charter_role NOT NULL DEFAULT 'captain',
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_ccm_crew FOREIGN KEY (crew_id) REFERENCES crew(id) ON DELETE CASCADE,
  CONSTRAINT fk_ccm_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT uq_crew_charter_member UNIQUE (crew_id, charter_id)
);
CREATE INDEX IF NOT EXISTS idx_ccm_charter ON crew_charter_member(charter_id);
CREATE INDEX IF NOT EXISTS idx_ccm_crew ON crew_charter_member(crew_id);
CREATE INDEX IF NOT EXISTS idx_ccm_charter_role ON crew_charter_member(charter_id, charter_role);

-- project_applications
CREATE TABLE IF NOT EXISTS project_applications (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  project_id uuid NOT NULL,
  name text NOT NULL,
  slug text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_app_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_app_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT fk_app_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE CASCADE,
  CONSTRAINT uq_project_slug UNIQUE (project_id, slug)
);
CREATE INDEX IF NOT EXISTS idx_app_vessel ON project_applications(vessel_id);
CREATE INDEX IF NOT EXISTS idx_app_charter ON project_applications(charter_id);
CREATE INDEX IF NOT EXISTS idx_app_project ON project_applications(project_id);
CREATE INDEX IF NOT EXISTS idx_app_deleted ON project_applications(deleted_at);

-- project_databases
CREATE TABLE IF NOT EXISTS project_databases (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  project_id uuid NOT NULL,
  engine text NOT NULL,
  plan text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_db_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_db_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT fk_db_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_db_vessel ON project_databases(vessel_id);
CREATE INDEX IF NOT EXISTS idx_db_charter ON project_databases(charter_id);
CREATE INDEX IF NOT EXISTS idx_db_project ON project_databases(project_id);
CREATE INDEX IF NOT EXISTS idx_db_engine_plan ON project_databases(engine, plan);

-- project_api_gateway
CREATE TABLE IF NOT EXISTS project_api_gateway (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  project_id uuid NOT NULL,
  engine api_gateway_engine NOT NULL DEFAULT 'traefik',
  configuration jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  CONSTRAINT fk_gw_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_gw_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT fk_gw_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE CASCADE,
  CONSTRAINT uq_api_gw_project UNIQUE (project_id)
);
CREATE INDEX IF NOT EXISTS idx_api_gw_updated ON project_api_gateway(updated_at);
-- JSONB GIN for configuration search
CREATE INDEX IF NOT EXISTS idx_api_gw_configuration_gin ON project_api_gateway USING gin (configuration jsonb_path_ops);

-- logbook
CREATE TABLE IF NOT EXISTS logbook (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid,
  user_id uuid,
  action text NOT NULL,
  object_type text NOT NULL,
  object_id uuid,
  details jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_logbook_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
  CONSTRAINT fk_logbook_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE NO ACTION,
  CONSTRAINT fk_logbook_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_logbook_vessel_time ON logbook(vessel_id, created_at);
CREATE INDEX IF NOT EXISTS idx_logbook_charter_time ON logbook(charter_id, created_at);
CREATE INDEX IF NOT EXISTS idx_logbook_user_time ON logbook(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_logbook_object_time ON logbook(object_type, object_id, created_at);
CREATE INDEX IF NOT EXISTS idx_logbook_details_gin ON logbook USING gin (details jsonb_path_ops);

-- locker
CREATE TABLE IF NOT EXISTS locker (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  crew_id uuid,
  name text NOT NULL,
  vault_mount text NOT NULL,
  vault_path text NOT NULL,
  kv_version int,
  key_id text,
  fingerprint text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  expires_at timestamptz,
  CONSTRAINT fk_locker_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_locker_crew FOREIGN KEY (crew_id) REFERENCES crew(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_locker_vessel ON locker(vessel_id);
CREATE INDEX IF NOT EXISTS idx_locker_crew ON locker(crew_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_locker_vessel_name ON locker(vessel_id, name);

-- project_secrets
CREATE TABLE IF NOT EXISTS project_secrets (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  project_id uuid NOT NULL,
  crew_id uuid,
  key text NOT NULL,
  vault_mount text NOT NULL,
  vault_path text NOT NULL,
  kv_version int,
  key_id text,
  fingerprint text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  expires_at timestamptz,
  CONSTRAINT fk_secrets_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_secrets_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT fk_secrets_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_secrets_crew FOREIGN KEY (crew_id) REFERENCES crew(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_secrets_vessel ON project_secrets(vessel_id);
CREATE INDEX IF NOT EXISTS idx_secrets_charter ON project_secrets(charter_id);
CREATE INDEX IF NOT EXISTS idx_secrets_project ON project_secrets(project_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_secrets_project_key ON project_secrets(project_id, key);
CREATE INDEX IF NOT EXISTS idx_secrets_expires ON project_secrets(expires_at);

-- project_configs
CREATE TABLE IF NOT EXISTS project_configs (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  project_id uuid NOT NULL,
  crew_id uuid,
  key text NOT NULL,
  value text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz,
  CONSTRAINT fk_configs_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_configs_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT fk_configs_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_configs_crew FOREIGN KEY (crew_id) REFERENCES crew(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_configs_vessel ON project_configs(vessel_id);
CREATE INDEX IF NOT EXISTS idx_configs_charter ON project_configs(charter_id);
CREATE INDEX IF NOT EXISTS idx_configs_project ON project_configs(project_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_configs_project_key ON project_configs(project_id, key);

-- vessel_billing_profile
CREATE TABLE IF NOT EXISTS vessel_billing_profile (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  company_name text,
  billing_to text,
  address1 text,
  address2 text,
  postal_code text,
  city text,
  state text,
  country text,
  email text,
  phone text,
  vat_number text,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_vessel_billing_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT uq_vessel_billing_vessel UNIQUE (vessel_id)
);

-- charter_billing_profile
CREATE TABLE IF NOT EXISTS charter_billing_profile (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  charter_id uuid NOT NULL,
  company_name text,
  billing_to text,
  address1 text,
  address2 text,
  postal_code text,
  city text,
  state text,
  country text,
  email text,
  phone text,
  vat_number text,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_charter_billing_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_charter_billing_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE CASCADE,
  CONSTRAINT uq_charter_billing_charter UNIQUE (vessel_id, charter_id)
);
CREATE INDEX IF NOT EXISTS idx_charter_billing_vessel ON charter_billing_profile(vessel_id);

-- sign_up_inquiries
CREATE TABLE IF NOT EXISTS sign_up_inquiries (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  user_id uuid NOT NULL,
  intent sign_up_intent NOT NULL DEFAULT 'exploring',
  technical_experience technical_experience NOT NULL DEFAULT 'non_tech',
  questions jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_signups_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE NO ACTION,
  CONSTRAINT fk_signups_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_signups_vessel ON sign_up_inquiries(vessel_id);
CREATE INDEX IF NOT EXISTS idx_signups_user ON sign_up_inquiries(user_id);
CREATE INDEX IF NOT EXISTS idx_signups_created ON sign_up_inquiries(created_at);
CREATE INDEX IF NOT EXISTS idx_signups_intent ON sign_up_inquiries(intent);
CREATE INDEX IF NOT EXISTS idx_signups_experience ON sign_up_inquiries(technical_experience);

-- vessel_engines
CREATE TABLE IF NOT EXISTS vessel_engines (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  name text NOT NULL,
  mode engine_mode NOT NULL DEFAULT 'managed_engine',
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_engines_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT uq_engines_vessel_name UNIQUE (vessel_id, name)
);
CREATE INDEX IF NOT EXISTS idx_engines_vessel ON vessel_engines(vessel_id);

-- vessel_engine_regions
CREATE TABLE IF NOT EXISTS vessel_engine_regions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  vessel_engine_id uuid NOT NULL,
  name text NOT NULL,
  provider_name text NOT NULL,
  geo_region geo_region NOT NULL,
  location text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_regions_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_regions_engine FOREIGN KEY (vessel_engine_id) REFERENCES vessel_engines(id) ON DELETE CASCADE,
  CONSTRAINT uq_regions_engine_name UNIQUE (vessel_engine_id, name)
);
CREATE INDEX IF NOT EXISTS idx_regions_vessel ON vessel_engine_regions(vessel_id);
CREATE INDEX IF NOT EXISTS idx_regions_engine ON vessel_engine_regions(vessel_engine_id);
CREATE INDEX IF NOT EXISTS idx_regions_geo ON vessel_engine_regions(geo_region);

-- vessel_engine_nodes
CREATE TABLE IF NOT EXISTS vessel_engine_nodes (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid NOT NULL,
  vessel_engine_id uuid NOT NULL,
  vessel_engine_region_id uuid NOT NULL,
  node_type node_type NOT NULL DEFAULT 'controller',
  deploy_mode node_deploy_mode NOT NULL DEFAULT 'applications_databases',
  name text NOT NULL,
  ip_address text NOT NULL,
  cpu text,
  memory text,
  storage text,
  provisioning boolean NOT NULL DEFAULT false,
  provisioning_locker_id uuid,
  provisioning_security_updates boolean NOT NULL DEFAULT false,
  provisioning_security_updates_schedule text,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_nodes_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_nodes_engine FOREIGN KEY (vessel_engine_id) REFERENCES vessel_engines(id) ON DELETE CASCADE,
  CONSTRAINT fk_nodes_region FOREIGN KEY (vessel_engine_region_id) REFERENCES vessel_engine_regions(id) ON DELETE CASCADE,
  CONSTRAINT fk_nodes_provisioning_locker FOREIGN KEY (provisioning_locker_id) REFERENCES locker(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_nodes_vessel ON vessel_engine_nodes(vessel_id);
CREATE INDEX IF NOT EXISTS idx_nodes_engine ON vessel_engine_nodes(vessel_engine_id);
CREATE INDEX IF NOT EXISTS idx_nodes_region ON vessel_engine_nodes(vessel_engine_region_id);
CREATE INDEX IF NOT EXISTS idx_nodes_type_mode ON vessel_engine_nodes(node_type, deploy_mode);
CREATE UNIQUE INDEX IF NOT EXISTS uq_nodes_ip ON vessel_engine_nodes(ip_address);
CREATE INDEX IF NOT EXISTS idx_nodes_provisioning_time ON vessel_engine_nodes(provisioning, created_at);

-- outbox_events
CREATE TABLE IF NOT EXISTS outbox_events (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid,
  charter_id uuid,
  project_id uuid,
  aggregate_table text NOT NULL,
  aggregate_id uuid,
  event_type text NOT NULL,
  event_key text,
  event_version int NOT NULL DEFAULT 1,
  payload jsonb NOT NULL,
  metadata jsonb,
  status outbox_status NOT NULL DEFAULT 'pending',
  attempts int NOT NULL DEFAULT 0,
  next_attempt_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz,
  error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_outbox_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE NO ACTION,
  CONSTRAINT fk_outbox_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE NO ACTION,
  CONSTRAINT fk_outbox_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE NO ACTION,
  CONSTRAINT uq_outbox_event_key UNIQUE (event_key)
);
CREATE INDEX IF NOT EXISTS idx_outbox_status_next ON outbox_events(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_time ON outbox_events(aggregate_table, aggregate_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_vessel_time ON outbox_events(vessel_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_charter_time ON outbox_events(charter_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_project_time ON outbox_events(project_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_type_time ON outbox_events(event_type, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_payload_gin ON outbox_events USING gin (payload jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_outbox_metadata_gin ON outbox_events USING gin (metadata jsonb_path_ops);

-- webhook_subscriptions
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id uuid,
  charter_id uuid,
  project_id uuid,
  url text NOT NULL,
  secret text,
  event_types text[],
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT fk_ws_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE NO ACTION,
  CONSTRAINT fk_ws_charter FOREIGN KEY (charter_id) REFERENCES charters(id) ON DELETE NO ACTION,
  CONSTRAINT fk_ws_project FOREIGN KEY (project_id) REFERENCES charter_projects(id) ON DELETE NO ACTION
);
CREATE INDEX IF NOT EXISTS idx_webhook_subscriptions_active ON webhook_subscriptions(active);
CREATE INDEX IF NOT EXISTS idx_webhook_scope ON webhook_subscriptions(vessel_id, charter_id, project_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_webhook_scope_url ON webhook_subscriptions(vessel_id, charter_id, project_id, url);

-- webhook_deliveries
CREATE TABLE IF NOT EXISTS webhook_deliveries (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  subscription_id uuid NOT NULL,
  outbox_event_id uuid NOT NULL,
  status delivery_status NOT NULL DEFAULT 'queued',
  http_status int,
  attempt_no int NOT NULL DEFAULT 0,
  duration_ms int,
  request_headers jsonb,
  request_body jsonb,
  response_headers jsonb,
  response_body text,
  error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_wd_subscription FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
  CONSTRAINT fk_wd_event FOREIGN KEY (outbox_event_id) REFERENCES outbox_events(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_subscription ON webhook_deliveries(subscription_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_event ON webhook_deliveries(outbox_event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_status_created ON webhook_deliveries(status, created_at);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_sub_time ON webhook_deliveries(subscription_id, created_at);

-- ==============================
-- Suggested Views / Materialized Views
-- ==============================

-- View: audit-friendly outbox projection with names where possible
CREATE OR REPLACE VIEW v_outbox_audit AS
SELECT
  e.id,
  e.created_at,
  e.status,
  e.event_type,
  e.aggregate_table,
  e.aggregate_id,
  e.event_key,
  e.event_version,
  e.payload,
  e.metadata,
  v.name       AS vessel_name,
  c.name       AS charter_name,
  p.name       AS project_name,
  p.environment AS project_env
FROM outbox_events e
LEFT JOIN vessels v ON v.id = e.vessel_id
LEFT JOIN charters c ON c.id = e.charter_id
LEFT JOIN charter_projects p ON p.id = e.project_id;

-- Materialized View: events ready to process (queue)
DROP MATERIALIZED VIEW IF EXISTS mv_outbox_ready;
CREATE MATERIALIZED VIEW mv_outbox_ready AS
SELECT e.*
FROM outbox_events e
WHERE e.status IN ('pending','failed') AND e.next_attempt_at <= now()
ORDER BY e.next_attempt_at, e.created_at;

-- Helpful index on MV
CREATE INDEX IF NOT EXISTS idx_mv_outbox_ready_next ON mv_outbox_ready(next_attempt_at);

-- View: recent logbook (last 30 days)
CREATE OR REPLACE VIEW v_logbook_recent AS
SELECT *
FROM logbook
WHERE created_at >= now() - interval '30 days';

-- Materialized View: recent logbook with common sort
DROP MATERIALIZED VIEW IF EXISTS mv_logbook_recent;
CREATE MATERIALIZED VIEW mv_logbook_recent AS
SELECT *
FROM logbook
WHERE created_at >= now() - interval '30 days'
ORDER BY created_at DESC;

CREATE INDEX IF NOT EXISTS idx_mv_logbook_recent_created ON mv_logbook_recent(created_at);

-- Optional: email trigram search (useful for fuzzy lookups)
CREATE INDEX IF NOT EXISTS idx_users_email_trgm ON users USING gin (email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_user_identities_email_trgm ON user_identities USING gin (email gin_trgm_ops);

-- Done.

-- Galley Outbox Trigger Pack
-- This file adds generic outbox-enqueue triggers for domain tables.
-- Safe to run multiple times (guarded by CREATE OR REPLACE + IF NOT EXISTS patterns).

-- =========================================
-- Request context helpers (actor, ip, ua, correlation)
-- =========================================

-- Usage from app (per TX/session):
--   SELECT galley_set_request_context(actor_id := '...', ip := '1.2.3.4', user_agent := '...', correlation_id := '...');
-- These are readable in triggers via current_setting(..., true).

CREATE OR REPLACE FUNCTION galley_set_request_context(
  actor_id uuid DEFAULT NULL,
  ip text DEFAULT NULL,
  user_agent text DEFAULT NULL,
  correlation_id text DEFAULT NULL
) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF actor_id IS NOT NULL THEN PERFORM set_config('galley.actor_id', actor_id::text, true); END IF;
  IF ip IS NOT NULL THEN PERFORM set_config('galley.ip', ip, true); END IF;
  IF user_agent IS NOT NULL THEN PERFORM set_config('galley.user_agent', user_agent, true); END IF;
  IF correlation_id IS NOT NULL THEN PERFORM set_config('galley.correlation_id', correlation_id, true); END IF;
END;
$$;

-- =========================================
-- Generic outbox enqueue helper
-- =========================================
CREATE OR REPLACE FUNCTION galley_outbox_enqueue(
  _aggregate_table text,
  _aggregate_id uuid,
  _event_type text,
  _payload jsonb,
  _vessel_id uuid DEFAULT NULL,
  _charter_id uuid DEFAULT NULL,
  _project_id uuid DEFAULT NULL,
  _event_key text DEFAULT NULL
) RETURNS void LANGUAGE sql AS $$
  INSERT INTO outbox_events (
    vessel_id, charter_id, project_id,
    aggregate_table, aggregate_id, event_type, event_key, payload, metadata,
    status, attempts, next_attempt_at, created_at
  )
  VALUES (
    _vessel_id, _charter_id, _project_id,
    _aggregate_table, _aggregate_id, _event_type, _event_key, _payload,
    jsonb_strip_nulls(jsonb_build_object(
      'actor_id', current_setting('galley.actor_id', true),
      'ip', current_setting('galley.ip', true),
      'user_agent', current_setting('galley.user_agent', true),
      'correlation_id', current_setting('galley.correlation_id', true)
    )),
    'pending', 0, now(), now()
  )
  ON CONFLICT (event_key) DO NOTHING;
$$;

-- =========================================
-- Generic trigger function
-- - Infers scope (vessel_id/charter_id/project_id) if those columns exist
-- - Builds payload as: INSERT => {after}, UPDATE => {before, after}, DELETE => {before}
-- =========================================
CREATE OR REPLACE FUNCTION galley_outbox_trigger() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  agg_table text := TG_TABLE_NAME;
  rec_json jsonb;
  vessel_id uuid;
  charter_id uuid;
  project_id uuid;
  event_type text;
  payload jsonb;
  agg_id uuid;
BEGIN
  IF TG_OP = 'INSERT' THEN
    rec_json := to_jsonb(NEW);
    agg_id := NEW.id;
    event_type := 'INSERT';
    payload := jsonb_build_object('after', rec_json);
  ELSIF TG_OP = 'UPDATE' THEN
    rec_json := to_jsonb(NEW);
    agg_id := NEW.id;
    event_type := 'UPDATE';
    payload := jsonb_build_object('before', to_jsonb(OLD), 'after', rec_json);
  ELSIF TG_OP = 'DELETE' THEN
    rec_json := to_jsonb(OLD);
    agg_id := OLD.id;
    event_type := 'DELETE';
    payload := jsonb_build_object('before', rec_json);
  ELSE
    RETURN NULL;
  END IF;

  -- Best-effort scope extraction from row JSON
  vessel_id  := NULLIF((rec_json->>'vessel_id'),'')::uuid;
  charter_id := NULLIF((rec_json->>'charter_id'),'')::uuid;
  project_id := NULLIF((rec_json->>'project_id'),'')::uuid;

  PERFORM galley_outbox_enqueue(
    agg_table,
    agg_id,
    event_type,
    payload,
    vessel_id,
    charter_id,
    project_id,
    NULL -- event_key; provide if you need idempotency across retries
  );

  RETURN NULL;
END;
$$;

-- =========================================
-- Attach triggers to domain tables
-- Avoid attaching to outbox_* and webhook_* to prevent recursion.
-- =========================================

-- Helper: create trigger if not exists (PG lacks native IF NOT EXISTS for triggers).
-- We emulate with a DO block checking pg_trigger.
DO $$
DECLARE
  r record;
BEGIN
  -- List of tables to attach the trigger to (schema public assumed).
  FOR r IN SELECT unnest(ARRAY[
    'users',
    'vessels',
    'charters',
    'charter_projects',
    'crew',
    'crew_charter_member',
    'project_applications',
    'project_databases',
    'project_api_gateway',
    'project_secrets',
    'project_configs',
    'vessel_engines',
    'vessel_engine_regions',
    'vessel_engine_nodes',
    'locker',
    'logbook'  -- include if you want log entries to also emit outbox events
  ]) AS tbl
  LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_trigger
      WHERE tgname = format('trg_outbox_%s', r.tbl)
    ) THEN
      EXECUTE format('
        CREATE TRIGGER %I
        AFTER INSERT OR UPDATE OR DELETE ON %I
        FOR EACH ROW
        EXECUTE FUNCTION galley_outbox_trigger();
      ', format('trg_outbox_%s', r.tbl), r.tbl);
    END IF;
  END LOOP;
END $$;

-- =========================================
-- Optional: lightweight MV refresh helper for workers
-- =========================================
CREATE OR REPLACE FUNCTION galley_refresh_mv_outbox_ready() RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
  -- Use CONCURRENTLY if MV was created WITH UNIQUE INDEX; ours is not, so plain refresh.
  REFRESH MATERIALIZED VIEW mv_outbox_ready;
END;
$$;



-- Galley – RLS & Access Helpers
-- Inspired by earlier tenant-centric SQL (session GUCs + policies)
-- Safe to run after galley_schema.sql

-- ==============================
-- Extensions (needed by some setups)
-- ==============================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ==============================
-- Session/Auth helpers
-- ==============================
-- Set the authenticated user id for the session/tx
CREATE OR REPLACE FUNCTION galley_set_auth_context(_user_id uuid) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
  PERFORM set_config('galley.user_id', COALESCE(_user_id::text,''), true);
END;
$$;

CREATE OR REPLACE FUNCTION galley_current_user_id() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('galley.user_id', true), '')::uuid;
$$;

-- Has access checks (by membership)
CREATE OR REPLACE FUNCTION galley_has_vessel_access(_vessel_id uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT EXISTS(
    SELECT 1 FROM crew c
    WHERE c.vessel_id = _vessel_id
      AND c.user_id = galley_current_user_id()
      AND c.status = 'active'
  );
$$;

CREATE OR REPLACE FUNCTION galley_has_charter_access(_charter_id uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT EXISTS(
    SELECT 1
    FROM crew_charter_member m
    JOIN crew c ON c.id = m.crew_id
    WHERE m.charter_id = _charter_id
      AND c.user_id = galley_current_user_id()
      AND c.status = 'active'
  );
$$;

CREATE OR REPLACE FUNCTION galley_has_project_access(_project_id uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT EXISTS(
    SELECT 1
    FROM charter_projects p
    JOIN crew_charter_member m ON m.charter_id = p.charter_id
    JOIN crew c ON c.id = m.crew_id
    WHERE p.id = _project_id
      AND c.user_id = galley_current_user_id()
      AND c.status = 'active'
  );
$$;

-- ==============================
-- RLS: enable & policies
-- ==============================
-- Enable RLS on key tables
DO $$ BEGIN
  PERFORM 1 FROM pg_class WHERE relname = 'vessels';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE vessels ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'crew';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE crew ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'charters';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE charters ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'crew_charter_member';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE crew_charter_member ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'charter_projects';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE charter_projects ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'project_applications';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE project_applications ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'project_databases';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE project_databases ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'project_api_gateway';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE project_api_gateway ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'project_secrets';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE project_secrets ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'project_configs';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE project_configs ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'locker';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE locker ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'logbook';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE logbook ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'outbox_events';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'webhook_subscriptions';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE webhook_subscriptions ENABLE ROW LEVEL SECURITY';
  END IF;
  PERFORM 1 FROM pg_class WHERE relname = 'webhook_deliveries';
  IF FOUND THEN
    EXECUTE 'ALTER TABLE webhook_deliveries ENABLE ROW LEVEL SECURITY';
  END IF;
END $$;

-- Vessels: zichtbaar voor crew, muteren voor eigen crew
CREATE POLICY p_vessels_select ON vessels
  FOR SELECT USING (galley_has_vessel_access(id));
CREATE POLICY p_vessels_insert ON vessels
  FOR INSERT WITH CHECK (galley_current_user_id() IS NOT NULL);
CREATE POLICY p_vessels_update ON vessels
  FOR UPDATE USING (galley_has_vessel_access(id));
CREATE POLICY p_vessels_delete ON vessels
  FOR DELETE USING (galley_has_vessel_access(id));

-- Crew: je ziet eigen crewrecords binnen vessels waar je toegang toe hebt
CREATE POLICY p_crew_select ON crew
  FOR SELECT USING (galley_has_vessel_access(vessel_id));
CREATE POLICY p_crew_insert ON crew
  FOR INSERT WITH CHECK (galley_has_vessel_access(vessel_id));
CREATE POLICY p_crew_update ON crew
  FOR UPDATE USING (galley_has_vessel_access(vessel_id));
CREATE POLICY p_crew_delete ON crew
  FOR DELETE USING (galley_has_vessel_access(vessel_id));

-- Charters: leden van vessel zien charters; muteren door leden
CREATE POLICY p_charters_select ON charters
  FOR SELECT USING (vessel_id IS NULL OR galley_has_vessel_access(vessel_id));
CREATE POLICY p_charters_insert ON charters
  FOR INSERT WITH CHECK (vessel_id IS NULL OR galley_has_vessel_access(vessel_id));
CREATE POLICY p_charters_update ON charters
  FOR UPDATE USING (vessel_id IS NULL OR galley_has_vessel_access(vessel_id));
CREATE POLICY p_charters_delete ON charters
  FOR DELETE USING (vessel_id IS NULL OR galley_has_vessel_access(vessel_id));

-- Crew charter membership: zichtbaar voor betrokkenen
CREATE POLICY p_ccm_select ON crew_charter_member
  FOR SELECT USING (
    galley_has_charter_access(charter_id)
    OR EXISTS (SELECT 1 FROM crew c WHERE c.id = crew_charter_member.crew_id AND c.user_id = galley_current_user_id())
  );
CREATE POLICY p_ccm_insert ON crew_charter_member
  FOR INSERT WITH CHECK (galley_has_charter_access(charter_id));
CREATE POLICY p_ccm_update ON crew_charter_member
  FOR UPDATE USING (galley_has_charter_access(charter_id));
CREATE POLICY p_ccm_delete ON crew_charter_member
  FOR DELETE USING (galley_has_charter_access(charter_id));

-- Projects & resources
CREATE POLICY p_projects_select ON charter_projects
  FOR SELECT USING (galley_has_charter_access(charter_id));
CREATE POLICY p_projects_insert ON charter_projects
  FOR INSERT WITH CHECK (galley_has_charter_access(charter_id));
CREATE POLICY p_projects_update ON charter_projects
  FOR UPDATE USING (galley_has_charter_access(charter_id));
CREATE POLICY p_projects_delete ON charter_projects
  FOR DELETE USING (galley_has_charter_access(charter_id));

CREATE POLICY p_applications_select ON project_applications
  FOR SELECT USING (galley_has_project_access(project_id));
CREATE POLICY p_applications_insert ON project_applications
  FOR INSERT WITH CHECK (galley_has_project_access(project_id));
CREATE POLICY p_applications_update ON project_applications
  FOR UPDATE USING (galley_has_project_access(project_id));
CREATE POLICY p_applications_delete ON project_applications
  FOR DELETE USING (galley_has_project_access(project_id));

CREATE POLICY p_databases_select ON project_databases
  FOR SELECT USING (galley_has_project_access(project_id));
CREATE POLICY p_databases_insert ON project_databases
  FOR INSERT WITH CHECK (galley_has_project_access(project_id));
CREATE POLICY p_databases_update ON project_databases
  FOR UPDATE USING (galley_has_project_access(project_id));
CREATE POLICY p_databases_delete ON project_databases
  FOR DELETE USING (galley_has_project_access(project_id));

CREATE POLICY p_gw_select ON project_api_gateway
  FOR SELECT USING (galley_has_project_access(project_id));
CREATE POLICY p_gw_insert ON project_api_gateway
  FOR INSERT WITH CHECK (galley_has_project_access(project_id));
CREATE POLICY p_gw_update ON project_api_gateway
  FOR UPDATE USING (galley_has_project_access(project_id));
CREATE POLICY p_gw_delete ON project_api_gateway
  FOR DELETE USING (galley_has_project_access(project_id));

CREATE POLICY p_secrets_select ON project_secrets
  FOR SELECT USING (galley_has_project_access(project_id));
CREATE POLICY p_secrets_insert ON project_secrets
  FOR INSERT WITH CHECK (galley_has_project_access(project_id));
CREATE POLICY p_secrets_update ON project_secrets
  FOR UPDATE USING (galley_has_project_access(project_id));
CREATE POLICY p_secrets_delete ON project_secrets
  FOR DELETE USING (galley_has_project_access(project_id));

CREATE POLICY p_configs_select ON project_configs
  FOR SELECT USING (galley_has_project_access(project_id));
CREATE POLICY p_configs_insert ON project_configs
  FOR INSERT WITH CHECK (galley_has_project_access(project_id));
CREATE POLICY p_configs_update ON project_configs
  FOR UPDATE USING (galley_has_project_access(project_id));
CREATE POLICY p_configs_delete ON project_configs
  FOR DELETE USING (galley_has_project_access(project_id));

-- Locker: op vessel-niveau
CREATE POLICY p_locker_select ON locker
  FOR SELECT USING (galley_has_vessel_access(vessel_id));
CREATE POLICY p_locker_insert ON locker
  FOR INSERT WITH CHECK (galley_has_vessel_access(vessel_id));
CREATE POLICY p_locker_update ON locker
  FOR UPDATE USING (galley_has_vessel_access(vessel_id));
CREATE POLICY p_locker_delete ON locker
  FOR DELETE USING (galley_has_vessel_access(vessel_id));

-- Logbook: leesbaar als je de vessel ziet
CREATE POLICY p_logbook_select ON logbook
  FOR SELECT USING (galley_has_vessel_access(vessel_id));
-- Mutaties in logbook gebeuren via server-side functies; geen directe INSERT/UPDATE/DELETE policies nodig.

-- Outbox/Webhooks: read within scope; writes via triggers/service
CREATE POLICY p_outbox_select ON outbox_events
  FOR SELECT USING (
    (vessel_id IS NULL OR galley_has_vessel_access(vessel_id))
    AND (charter_id IS NULL OR galley_has_charter_access(charter_id))
    AND (project_id IS NULL OR galley_has_project_access(project_id))
  );

CREATE POLICY p_webhook_subs_select ON webhook_subscriptions
  FOR SELECT USING (
    (vessel_id IS NULL OR galley_has_vessel_access(vessel_id))
    AND (charter_id IS NULL OR galley_has_charter_access(charter_id))
    AND (project_id IS NULL OR galley_has_project_access(project_id))
  );
CREATE POLICY p_webhook_subs_insert ON webhook_subscriptions
  FOR INSERT WITH CHECK (
    (vessel_id IS NULL OR galley_has_vessel_access(vessel_id))
    AND (charter_id IS NULL OR galley_has_charter_access(charter_id))
    AND (project_id IS NULL OR galley_has_project_access(project_id))
  );
CREATE POLICY p_webhook_subs_update ON webhook_subscriptions
  FOR UPDATE USING (
    (vessel_id IS NULL OR galley_has_vessel_access(vessel_id))
    AND (charter_id IS NULL OR galley_has_charter_access(charter_id))
    AND (project_id IS NULL OR galley_has_project_access(project_id))
  );
CREATE POLICY p_webhook_subs_delete ON webhook_subscriptions
  FOR DELETE USING (
    (vessel_id IS NULL OR galley_has_vessel_access(vessel_id))
    AND (charter_id IS NULL OR galley_has_charter_access(charter_id))
    AND (project_id IS NULL OR galley_has_project_access(project_id))
  );

CREATE POLICY p_webhook_deliveries_select ON webhook_deliveries
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM webhook_subscriptions s
      WHERE s.id = webhook_deliveries.subscription_id
        AND (s.vessel_id IS NULL OR galley_has_vessel_access(s.vessel_id))
        AND (s.charter_id IS NULL OR galley_has_charter_access(s.charter_id))
        AND (s.project_id IS NULL OR galley_has_project_access(s.project_id))
    )
  );
