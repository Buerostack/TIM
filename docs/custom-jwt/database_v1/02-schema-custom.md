# Schema: custom

## Tables

### 1) `custom.jwt_metadata`

Stores metadata for custom JWTs that TIM has issued.

| Column           | Type                 | Notes |
|------------------|----------------------|-------|
| id               | bigserial PRIMARY KEY| Surrogate key |
| jwt_name         | varchar(128)         | Logical cookie/header name used for the token |
| subject          | varchar(256)         | Optional `sub` claim |
| issued_at        | timestamptz NOT NULL | Server issue time |
| expires_at       | timestamptz NOT NULL | Expiration time |
| audience         | varchar(256)         | Optional `aud` |
| issuer           | varchar(256)         | Optional `iss` |
| claims_json      | jsonb NOT NULL       | Full claim set (without signature) |
| token_hash       | varchar(128)         | Optional short hash for audit (never the full token) |

Recommended indexes:
```sql
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_expires_at ON custom.jwt_metadata(expires_at);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_subject ON custom.jwt_metadata(subject);
CREATE INDEX IF NOT EXISTS idx_custom_jwt_metadata_jwt_name ON custom.jwt_metadata(jwt_name);
```

### 2) `custom.allowlist`

Optional allow-list: tokens (by hash) or subjects that are explicitly allowed.

| Column       | Type                 | Notes |
|--------------|----------------------|-------|
| id           | bigserial PRIMARY KEY| |
| token_hash   | varchar(128)         | Nullable; hash of token if used |
| subject      | varchar(256)         | Nullable |
| reason       | varchar(512)         | Optional note |
| created_at   | timestamptz NOT NULL DEFAULT now() | |

### 3) `custom.denylist`

Deny-list of revoked tokens or banned subjects. Denylist takes precedence over allowlist.

| Column       | Type                 | Notes |
|--------------|----------------------|-------|
| id           | bigserial PRIMARY KEY| |
| token_hash   | varchar(128)         | Nullable; hash of token if used |
| subject      | varchar(256)         | Nullable |
| reason       | varchar(512)         | Optional note |
| created_at   | timestamptz NOT NULL DEFAULT now() | |

Indexes:
```sql
CREATE INDEX IF NOT EXISTS idx_custom_denylist_token_hash ON custom.denylist(token_hash);
CREATE INDEX IF NOT EXISTS idx_custom_denylist_subject ON custom.denylist(subject);
```
