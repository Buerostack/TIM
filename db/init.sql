CREATE SCHEMA IF NOT EXISTS custom;
CREATE SCHEMA IF NOT EXISTS tara;

CREATE TABLE IF NOT EXISTS custom.denylist (
  jwt_uuid uuid PRIMARY KEY,
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_custom_denylist_exp ON custom.denylist (expires_at);

CREATE TABLE IF NOT EXISTS custom.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS tara.denylist (
  jwt_uuid uuid PRIMARY KEY,
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tara_denylist_exp ON tara.denylist (expires_at);

CREATE TABLE IF NOT EXISTS tara.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS tara.oauth_state (
  state text PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  pkce_verifier text
);
