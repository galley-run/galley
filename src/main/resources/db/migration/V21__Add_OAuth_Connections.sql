-- Add OAuth Connection Provider Enum
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'oauth_provider') THEN
    CREATE TYPE oauth_provider AS ENUM (
      'github',
      'gitlab',
      'bitbucket',
      'docker_hub',
      'github_registry',
      'gitlab_registry',
      'digitalocean_registry',
      'google',
      'microsoft'
    );
  END IF;
END $$;

-- Add OAuth Connection Type Enum
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'oauth_connection_type') THEN
    CREATE TYPE oauth_connection_type AS ENUM (
      'git_repository',
      'docker_registry',
      'social_login'
    );
  END IF;
END $$;

-- Add OAuth Connection Status Enum
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'oauth_connection_status') THEN
    CREATE TYPE oauth_connection_status AS ENUM (
      'active',
      'expired',
      'revoked'
    );
  END IF;
END $$;

-- Create oauth_connections table
CREATE TABLE IF NOT EXISTS oauth_connections (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  vessel_id UUID NOT NULL,
  user_id UUID NOT NULL,
  provider oauth_provider NOT NULL,
  connection_type oauth_connection_type NOT NULL,
  status oauth_connection_status NOT NULL DEFAULT 'active',

  -- Display information (visible to others with permissions)
  name TEXT NOT NULL,
  description TEXT,
  account_login TEXT,
  account_name TEXT,
  account_avatar_url TEXT,

  -- OAuth credentials (only accessible by the owning user)
  installation_id BIGINT,
  access_token_encrypted TEXT NOT NULL,
  refresh_token_encrypted TEXT,
  token_expires_at TIMESTAMPTZ,

  -- Scopes and permissions
  scopes TEXT[],

  -- Metadata
  metadata JSONB,

  -- Timestamps
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_oauth_connections_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE CASCADE,
  CONSTRAINT fk_oauth_connections_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient lookups
CREATE INDEX idx_oauth_connections_vessel_id ON oauth_connections(vessel_id);
CREATE INDEX idx_oauth_connections_user_id ON oauth_connections(user_id);
CREATE INDEX idx_oauth_connections_vessel_user ON oauth_connections(vessel_id, user_id);
CREATE INDEX idx_oauth_connections_provider ON oauth_connections(provider);
CREATE INDEX idx_oauth_connections_type ON oauth_connections(connection_type);
CREATE INDEX idx_oauth_connections_status ON oauth_connections(status);
CREATE INDEX idx_oauth_connections_vessel_status ON oauth_connections(vessel_id, status);
CREATE INDEX idx_oauth_connections_installation_id ON oauth_connections(installation_id) WHERE installation_id IS NOT NULL;

-- Unique constraint: one active connection per user per provider per vessel
CREATE UNIQUE INDEX uq_oauth_connections_active ON oauth_connections(vessel_id, user_id, provider, connection_type)
  WHERE status = 'active';

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_oauth_connections_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at
CREATE TRIGGER trigger_oauth_connections_updated_at
  BEFORE UPDATE ON oauth_connections
  FOR EACH ROW
  EXECUTE FUNCTION update_oauth_connections_updated_at();

-- Comments for documentation
COMMENT ON TABLE oauth_connections IS 'Stores OAuth connections for git repositories, docker registries, and social login. Tokens are encrypted and only accessible by the owning user, while metadata is visible to vessel members with appropriate permissions.';
COMMENT ON COLUMN oauth_connections.access_token_encrypted IS 'Encrypted access token - only accessible by the owning user';
COMMENT ON COLUMN oauth_connections.refresh_token_encrypted IS 'Encrypted refresh token - only accessible by the owning user';
COMMENT ON COLUMN oauth_connections.installation_id IS 'GitHub/GitLab App installation ID';
COMMENT ON COLUMN oauth_connections.metadata IS 'Provider-specific metadata (e.g., repository list, registry URL, app details)';
