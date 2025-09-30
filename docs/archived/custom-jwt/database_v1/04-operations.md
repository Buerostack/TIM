# Operations & Maintenance

## Initialization (no Liquibase)

Use a single SQL file to create schemas and tables (see `db/init.sql`). Example structure:

```sql
-- schemas
CREATE SCHEMA IF NOT EXISTS custom;
CREATE SCHEMA IF NOT EXISTS tara;

-- custom.jwt_metadata
CREATE TABLE IF NOT EXISTS custom.jwt_metadata (
  id          bigserial PRIMARY KEY,
  jwt_name    varchar(128) NOT NULL,
  subject     varchar(256),
  issued_at   timestamptz NOT NULL,
  expires_at  timestamptz NOT NULL,
  audience    varchar(256),
  issuer      varchar(256),
  claims_json jsonb NOT NULL,
  token_hash  varchar(128)
);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_expires_at ON custom.jwt_metadata(expires_at);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_subject ON custom.jwt_metadata(subject);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_jwt_name ON custom.jwt_metadata(jwt_name);

-- custom.allowlist
CREATE TABLE IF NOT EXISTS custom.allowlist (
  id         bigserial PRIMARY KEY,
  token_hash varchar(128),
  subject    varchar(256),
  reason     varchar(512),
  created_at timestamptz NOT NULL DEFAULT now()
);

-- custom.denylist
CREATE TABLE IF NOT EXISTS custom.denylist (
  id         bigserial PRIMARY KEY,
  token_hash varchar(128),
  subject    varchar(256),
  reason     varchar(512),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_custom_denylist_token_hash ON custom.denylist(token_hash);
CREATE INDEX IF NOT EXISTS idx_custom_denylist_subject ON custom.denylist(subject);

-- tara mirrors
CREATE TABLE IF NOT EXISTS tara.jwt_metadata (LIKE custom.jwt_metadata INCLUDING ALL);
CREATE INDEX IF NOT EXISTS idx_tara_jwt_metadata_expires_at ON tara.jwt_metadata(expires_at);
CREATE INDEX IF NOT EXISTS idx_tara_jwt_metadata_subject ON tara.jwt_metadata(subject);
CREATE INDEX IF NOT EXISTS idx_tara_jwt_metadata_jwt_name ON tara.jwt_metadata(jwt_name);

CREATE TABLE IF NOT EXISTS tara.allowlist (LIKE custom.allowlist INCLUDING ALL);
CREATE TABLE IF NOT EXISTS tara.denylist   (LIKE custom.denylist INCLUDING ALL);
CREATE INDEX IF NOT EXISTS idx_tara_denylist_token_hash ON tara.denylist(token_hash);
CREATE INDEX IF NOT EXISTS idx_tara_denylist_subject ON tara.denylist(subject);

CREATE TABLE IF NOT EXISTS tara.oauth_state (
  state        varchar(128) PRIMARY KEY,
  created_at   timestamptz NOT NULL DEFAULT now(),
  expires_at   timestamptz NOT NULL,
  nonce        varchar(128),
  redirect_uri varchar(512)
);
CREATE INDEX IF NOT EXISTS idx_tara_oauth_state_expires_at ON tara.oauth_state(expires_at);
```

In Docker, mount it via:
```yaml
volumes:
  - ./db/init.sql:/docker-entrypoint-initdb.d/00-init.sql:ro
```

## Common Queries

### Write metadata when issuing a token
```sql
INSERT INTO custom.jwt_metadata (jwt_name, subject, issued_at, expires_at, audience, issuer, claims_json, token_hash)
VALUES ('JWTTOKEN', 'user-123', now(), now() + interval '60 min', 'my-service', 'tim', '{"role":"viewer"}', 'sha256:...');
```

### Denylist a token by hash
```sql
INSERT INTO custom.denylist (token_hash, reason) VALUES ('sha256:abcd1234', 'User revoked access');
```

### Check if a token hash is denylisted
```sql
SELECT 1 FROM custom.denylist WHERE token_hash = 'sha256:abcd1234' LIMIT 1;
```

### Prune expired metadata
```sql
DELETE FROM custom.jwt_metadata WHERE expires_at < now() - interval '7 days';
DELETE FROM tara.jwt_metadata   WHERE expires_at < now() - interval '7 days';
DELETE FROM tara.oauth_state    WHERE expires_at < now();
```

## Admin & Backups

- **Backups**: Use standard `pg_dump`/`pg_restore`.  
  ```bash
  pg_dump -h localhost -U tim -d tim -Fc -f tim.dump
  pg_restore -h localhost -U tim -d tim -c tim.dump
  ```
- **Migrations**: For now we are Liquibase-free. Manage schema via SQL files and bump versions in `db/` folder (`V001__init.sql`, `V002__index_tuning.sql`, …) even if applied manually.
- **Observability**: Add simple health checks; consider Micrometer metrics in the app for issuance counts and denylist hits.

## Security Notes

- Never store full tokens in DB — store **hashes** for denylist/allowlist and audit.
- Keep `custom` and `tara` data separated for least privilege and blast radius reduction.
- Ensure DB role `tim` has only the necessary privileges on `custom.*` and `tara.*`.
