-- Test schema setup for validation tests
-- This script ensures the schema exists for testing

-- Create schemas if they don't exist
CREATE SCHEMA IF NOT EXISTS custom_jwt;
CREATE SCHEMA IF NOT EXISTS auth;

-- Create custom_jwt.jwt_metadata table
CREATE TABLE IF NOT EXISTS custom_jwt.jwt_metadata (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  jwt_uuid uuid NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL,
  subject text,
  jwt_name text,
  audience text,
  issuer text,
  supersedes uuid,
  original_jwt_uuid uuid NOT NULL
);

-- Create custom_jwt.denylist table
CREATE TABLE IF NOT EXISTS custom_jwt.denylist (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);

-- Create auth.jwt_metadata table
CREATE TABLE IF NOT EXISTS auth.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL
);

-- Create auth.denylist table
CREATE TABLE IF NOT EXISTS auth.denylist (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);

-- Create auth.oauth_state table
CREATE TABLE IF NOT EXISTS auth.oauth_state (
  state text PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  pkce_verifier text
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_subject ON custom_jwt.jwt_metadata (subject);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_issued ON custom_jwt.jwt_metadata (issued_at);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_jwt_uuid ON custom_jwt.jwt_metadata (jwt_uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_original ON custom_jwt.jwt_metadata (original_jwt_uuid);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_denylist_exp ON custom_jwt.denylist (expires_at);
CREATE INDEX IF NOT EXISTS idx_auth_denylist_exp ON auth.denylist (expires_at);