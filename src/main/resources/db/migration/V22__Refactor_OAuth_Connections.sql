-- Refactor OAuth Connections into separate tables for better permission model
-- This migration splits the monolithic oauth_connections into:
-- 1. oauth_connections: connection metadata (refactored)
-- 2. oauth_credentials: sensitive tokens and credentials (new)
-- 3. oauth_connection_grants: who can use/manage/revoke (new)

-- Drop existing table (it's empty)
DROP TABLE IF EXISTS oauth_connections CASCADE;

-- Drop and recreate the enums with new values
DROP TYPE IF EXISTS oauth_connection_status CASCADE;
DROP TYPE IF EXISTS oauth_connection_type CASCADE;
DROP TYPE IF EXISTS oauth_provider CASCADE;

-- OAuth Provider Enum
CREATE TYPE oauth_provider AS ENUM (
  'github',
  'gitlab',
  'bitbucket',
  'dockerhub',
  'ghcr'
);

-- OAuth Connection Type Enum
CREATE TYPE oauth_connection_type AS ENUM (
  'git',
  'registry'
);

-- OAuth Connection Status Enum
CREATE TYPE oauth_connection_status AS ENUM (
  'pending',
  'active',
  'revoked',
  'error'
);

-- OAuth Credential Kind Enum
CREATE TYPE oauth_credential_kind AS ENUM (
  'oauth_token',
  'github_app_installation',
  'pat'
);

-- OAuth Grant Permission Enum
CREATE TYPE oauth_grant_permission AS ENUM (
  'use',
  'manage',
  'revoke'
);

-- OAuth Grant Principal Type Enum
CREATE TYPE oauth_grant_principal_type AS ENUM (
  'user',
  'team',
  'role'
);

-- Main oauth_connections table
-- Stores connection metadata at charter/vessel level
CREATE TABLE oauth_connections (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Ownership: at charter or vessel level
  vessel_id UUID,
  charter_id UUID,

  -- Connection details
  type oauth_connection_type NOT NULL,
  provider oauth_provider NOT NULL,
  status oauth_connection_status NOT NULL DEFAULT 'pending',

  -- Display information
  display_name TEXT NOT NULL,

  -- Creator tracking
  created_by_user_id UUID NOT NULL,

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_validated_at TIMESTAMPTZ,

  -- OAuth metadata
  scopes JSONB,
  provider_account_id TEXT,

  -- Ensure connection is at vessel OR charter level, not both
  CONSTRAINT chk_oauth_connections_level CHECK (
    (vessel_id IS NOT NULL AND charter_id IS NULL) OR
    (vessel_id IS NULL AND charter_id IS NOT NULL)
  ),

  CONSTRAINT fk_oauth_connections_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_oauth_connections_user FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- oauth_credentials table
-- Stores sensitive tokens separately
CREATE TABLE oauth_credentials (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  connection_id UUID NOT NULL,

  -- Credential type
  credential_kind oauth_credential_kind NOT NULL,

  -- Tokens (encrypted)
  access_token_encrypted TEXT,
  refresh_token_encrypted TEXT,
  expires_at TIMESTAMPTZ,
  token_type TEXT,

  -- Provider-specific metadata
  provider_metadata JSONB,

  -- GitHub App specific fields
  installation_id BIGINT,
  app_id BIGINT,
  account_login TEXT,
  account_type TEXT,

  -- GitLab specific fields
  instance_url TEXT,

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_oauth_credentials_connection FOREIGN KEY (connection_id) REFERENCES oauth_connections(id) ON DELETE CASCADE
);

-- oauth_connection_grants table
-- Defines who can use/manage/revoke connections
CREATE TABLE oauth_connection_grants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  connection_id UUID NOT NULL,

  -- Principal (who gets the permission)
  principal_type oauth_grant_principal_type NOT NULL,
  principal_id TEXT NOT NULL,

  -- Permission level
  permission oauth_grant_permission NOT NULL,

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_oauth_grants_connection FOREIGN KEY (connection_id) REFERENCES oauth_connections(id) ON DELETE CASCADE,

  -- Unique grant per principal per connection
  CONSTRAINT uq_oauth_grants_principal UNIQUE (connection_id, principal_type, principal_id, permission)
);

-- Indexes for oauth_connections
CREATE INDEX idx_oauth_connections_vessel_id ON oauth_connections(vessel_id) WHERE vessel_id IS NOT NULL;
CREATE INDEX idx_oauth_connections_charter_id ON oauth_connections(charter_id) WHERE charter_id IS NOT NULL;
CREATE INDEX idx_oauth_connections_created_by ON oauth_connections(created_by_user_id);
CREATE INDEX idx_oauth_connections_provider ON oauth_connections(provider);
CREATE INDEX idx_oauth_connections_type ON oauth_connections(type);
CREATE INDEX idx_oauth_connections_status ON oauth_connections(status);
CREATE INDEX idx_oauth_connections_vessel_status ON oauth_connections(vessel_id, status) WHERE vessel_id IS NOT NULL;

-- Indexes for oauth_credentials
CREATE INDEX idx_oauth_credentials_connection_id ON oauth_credentials(connection_id);
CREATE INDEX idx_oauth_credentials_installation_id ON oauth_credentials(installation_id) WHERE installation_id IS NOT NULL;
CREATE INDEX idx_oauth_credentials_expires_at ON oauth_credentials(expires_at) WHERE expires_at IS NOT NULL;

-- Indexes for oauth_connection_grants
CREATE INDEX idx_oauth_grants_connection_id ON oauth_connection_grants(connection_id);
CREATE INDEX idx_oauth_grants_principal ON oauth_connection_grants(principal_type, principal_id);
CREATE INDEX idx_oauth_grants_permission ON oauth_connection_grants(permission);

-- Trigger to update updated_at on oauth_connections
CREATE OR REPLACE FUNCTION update_oauth_connections_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_oauth_connections_updated_at
  BEFORE UPDATE ON oauth_connections
  FOR EACH ROW
  EXECUTE FUNCTION update_oauth_connections_updated_at();

-- Trigger to update updated_at on oauth_credentials
CREATE OR REPLACE FUNCTION update_oauth_credentials_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_oauth_credentials_updated_at
  BEFORE UPDATE ON oauth_credentials
  FOR EACH ROW
  EXECUTE FUNCTION update_oauth_credentials_updated_at();

-- Comments for documentation
COMMENT ON TABLE oauth_connections IS 'OAuth integrations at charter/vessel level. Contains connection metadata and display information. Credentials are stored separately in oauth_credentials.';
COMMENT ON TABLE oauth_credentials IS 'Encrypted OAuth credentials and tokens. Separated from connections to allow fine-grained access control.';
COMMENT ON TABLE oauth_connection_grants IS 'Permission grants for OAuth connections. Defines who can use, manage, or revoke each connection.';

COMMENT ON COLUMN oauth_connections.display_name IS 'User-friendly name, e.g., "GitHub (Acme Org)"';
COMMENT ON COLUMN oauth_connections.provider_account_id IS 'External provider account/org ID';
COMMENT ON COLUMN oauth_connections.scopes IS 'OAuth scopes granted for this connection';

COMMENT ON COLUMN oauth_credentials.credential_kind IS 'Type of credential: oauth_token, github_app_installation, or pat';
COMMENT ON COLUMN oauth_credentials.provider_metadata IS 'Provider-specific metadata (repositories, webhooks, etc.)';
COMMENT ON COLUMN oauth_credentials.installation_id IS 'GitHub App installation ID';
COMMENT ON COLUMN oauth_credentials.instance_url IS 'GitLab instance URL for self-hosted instances';

COMMENT ON COLUMN oauth_connection_grants.principal_type IS 'Type of principal: user, team, or role';
COMMENT ON COLUMN oauth_connection_grants.principal_id IS 'ID of the principal (user_id, team_id, or role name)';
COMMENT ON COLUMN oauth_connection_grants.permission IS 'Permission level: use (can use connection), manage (can update settings), or revoke (can delete connection)';
