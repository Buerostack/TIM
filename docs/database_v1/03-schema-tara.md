# Schema: tara

## Tables

### 1) `tara.jwt_metadata`

Same structure and purpose as `custom.jwt_metadata`, but **only** for JWTs minted after successful TARA login.

Columns and indexes mirror `custom.jwt_metadata` for operational parity.

### 2) `tara.allowlist` and `tara.denylist`

Mirror the structures and semantics of the `custom` schema lists. Keep TARA tokens' lifecycle fully isolated.

### 3) `tara.oauth_state`

Short-lived state store used only during the OAuth2 redirect flow with TARA.

| Column       | Type                 | Notes |
|--------------|----------------------|-------|
| state        | varchar(128) PRIMARY KEY | CSRF state value sent to/returned by TARA |
| created_at   | timestamptz NOT NULL DEFAULT now() | |
| expires_at   | timestamptz NOT NULL | TTL for state (e.g., now()+10m) |
| nonce        | varchar(128)         | Optional nonce |
| redirect_uri | varchar(512)         | Where to return after login |

Index:
```sql
CREATE INDEX IF NOT EXISTS idx_tara_oauth_state_expires_at ON tara.oauth_state(expires_at);
```

> **Retention**: a background job should prune expired records regularly (see Operations).
