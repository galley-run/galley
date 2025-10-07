CREATE TABLE sessions (
  id                 uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id            uuid NOT NULL REFERENCES users(id),

  -- Only store the HASH, never the raw token
  refresh_token_hash bytea NOT NULL,   -- SHA-256(token + pepper)

  device_name        text,
  user_agent         text,
  ip_address         inet,

  issued_at          timestamptz NOT NULL DEFAULT now(),
  last_used_at       timestamptz NOT NULL DEFAULT now(),
  expires_at         timestamptz NOT NULL,

  revoked_at         timestamptz,
  replaced_by_id     uuid REFERENCES sessions(id),

  UNIQUE (user_id, refresh_token_hash)
);

CREATE INDEX ON sessions (user_id);
CREATE INDEX ON sessions (expires_at);
CREATE INDEX ON sessions (revoked_at) WHERE revoked_at IS NULL;
