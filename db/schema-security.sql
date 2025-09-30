-- Schema Security Setup for TIM
-- Creates separate database users with schema-specific access

-- Create dedicated database users
CREATE USER tim_custom_jwt WITH PASSWORD 'custom_jwt_secure_pass';
CREATE USER tim_auth WITH PASSWORD 'auth_secure_pass';

-- Grant schema-specific permissions
-- Custom JWT user can only access custom_jwt schema
GRANT USAGE ON SCHEMA custom_jwt TO tim_custom_jwt;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA custom_jwt TO tim_custom_jwt;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA custom_jwt TO tim_custom_jwt;

-- Auth user can only access auth schema
GRANT USAGE ON SCHEMA auth TO tim_auth;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO tim_auth;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA auth TO tim_auth;

-- Explicitly revoke access to the other schema
REVOKE ALL ON SCHEMA auth FROM tim_custom_jwt;
REVOKE ALL ON SCHEMA custom_jwt FROM tim_auth;

-- Remove default public schema access
REVOKE ALL ON SCHEMA public FROM tim_custom_jwt;
REVOKE ALL ON SCHEMA public FROM tim_auth;

-- Ensure future tables inherit permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA custom_jwt GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tim_custom_jwt;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tim_auth;