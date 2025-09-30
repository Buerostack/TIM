# TIM 2.0 Database Schema

## Overview
TIM 2.0 uses PostgreSQL with two main schemas:
- **custom_jwt**: Custom JWT token management
- **auth**: OAuth2 authentication support (TARA, Google, GitHub, etc.)

## Schema: custom_jwt

### Table: jwt_metadata
Stores metadata for all custom JWT tokens issued by TIM. Uses INSERT-only approach with extension chain tracking.

```sql
CREATE TABLE custom_jwt.jwt_metadata (
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
  supersedes uuid, -- Previous version this JWT replaces
  original_jwt_uuid uuid NOT NULL -- First JWT in the extension chain
);
```

**Indexes:**
- `idx_custom_jwt_metadata_subject` on `subject` - Fast lookups by user
- `idx_custom_jwt_metadata_issued` on `issued_at` - Chronological ordering
- `idx_custom_jwt_metadata_jwt_uuid` on `(jwt_uuid, created_at DESC)` - Find current version
- `idx_custom_jwt_metadata_original` on `original_jwt_uuid` - Extension chain queries

**Fields:**
- `id`: Primary key for this metadata record
- `jwt_uuid`: JWT identifier matching JWT `jti` claim (can have multiple versions)
- `created_at`: Database record creation timestamp (immutable)
- `claim_keys`: Comma-separated custom claim names
- `issued_at`: Token creation timestamp (from JWT)
- `expires_at`: Token expiration timestamp (from JWT)
- `subject`: User identifier (`sub` claim)
- `jwt_name`: Human-readable token name
- `audience`: Target audience (`aud` claim)
- `issuer`: Token issuer (`iss` claim)
- `supersedes`: Reference to previous version's `id` (for extensions)
- `original_jwt_uuid`: Reference to the first JWT in extension chain

**Extension Chain Logic:**
- New tokens: `original_jwt_uuid = jwt_uuid`, `supersedes = NULL`
- Extended tokens: `original_jwt_uuid = <original>`, `supersedes = <previous_id>`
- Current version: `SELECT * WHERE jwt_uuid = ? ORDER BY created_at DESC LIMIT 1`

### Table: denylist
Tracks revoked JWT tokens to prevent reuse.

```sql
CREATE TABLE custom_jwt.denylist (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);
```

**Indexes:**
- `idx_custom_jwt_denylist_exp` on `expires_at` - Cleanup expired entries

**Fields:**
- `jwt_uuid`: References `jwt_metadata.jwt_uuid`
- `created_at`: Database record creation timestamp (immutable)
- `denylisted_at`: Revocation timestamp
- `expires_at`: Original token expiration (for cleanup)
- `reason`: Optional revocation reason

## Schema: auth

### Table: jwt_metadata
Simplified metadata for OAuth2 tokens (TARA, Google, GitHub, etc.).

```sql
CREATE TABLE auth.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL
);
```

**Fields:**
- `jwt_uuid`: Unique token identifier
- `created_at`: Database record creation timestamp (immutable)
- `claim_keys`: Serialized token claims
- `issued_at`: Token creation timestamp
- `expires_at`: Token expiration timestamp

### Table: denylist
Revoked OAuth2 tokens tracking.

```sql
CREATE TABLE auth.denylist (
  jwt_uuid uuid PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);
```

**Indexes:**
- `idx_auth_denylist_exp` on `expires_at` - Cleanup expired entries

**Fields:**
- `jwt_uuid`: References `jwt_metadata.jwt_uuid`
- `created_at`: Database record creation timestamp (immutable)
- `denylisted_at`: Revocation timestamp
- `expires_at`: Original token expiration (for cleanup)
- `reason`: Optional revocation reason

### Table: oauth_state
OAuth2 state management for PKCE flows.

```sql
CREATE TABLE auth.oauth_state (
  state text PRIMARY KEY,
  created_at timestamp NOT NULL DEFAULT now(),
  pkce_verifier text
);
```

**Fields:**
- `state`: OAuth2 state parameter (CSRF protection)
- `created_at`: State creation timestamp
- `pkce_verifier`: PKCE code verifier for secure flows

## Data Flow

### Custom JWT Lifecycle
1. **Generation**: Insert into `custom_jwt.jwt_metadata` (original_jwt_uuid = jwt_uuid)
2. **Usage**: Find current version, validate against `custom_jwt.denylist`
3. **Extension**: Insert new version with supersedes reference, revoke old token
4. **Revocation**: Insert into `custom_jwt.denylist`
5. **Cleanup**: Remove expired entries from both tables

### OAuth2/TARA Flow
1. **Login Initiation**: Insert state into `auth.oauth_state`
2. **Callback**: Validate state, insert token into `auth.jwt_metadata`
3. **Token Usage**: Check against `auth.denylist`
4. **Logout**: Insert into `auth.denylist`

## Performance Considerations

### Indexes
- Subject-based queries use `idx_custom_jwt_metadata_subject`
- Chronological listing uses `idx_custom_jwt_metadata_issued`
- Expiration cleanup uses denylist expiration indexes

### Cleanup Strategy
```sql
-- Clean expired denylist entries
DELETE FROM custom_jwt.denylist WHERE expires_at < now();
DELETE FROM auth.denylist WHERE expires_at < now();

-- Clean expired OAuth states (recommend 1 hour TTL)
DELETE FROM auth.oauth_state WHERE created_at < now() - interval '1 hour';
```

### Query Patterns
```sql
-- Find current version of a JWT
SELECT * FROM custom_jwt.jwt_metadata
WHERE jwt_uuid = ?
ORDER BY created_at DESC
LIMIT 1;

-- Find user's active tokens (latest version of each)
SELECT DISTINCT ON (jwt_uuid) * FROM custom_jwt.jwt_metadata
WHERE subject = ?
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom_jwt.denylist)
  AND expires_at > now()
ORDER BY jwt_uuid, created_at DESC;

-- Find extension chain for a JWT
SELECT * FROM custom_jwt.jwt_metadata
WHERE original_jwt_uuid = ?
ORDER BY created_at ASC;

-- Validate token
SELECT 1 FROM (
  SELECT * FROM custom_jwt.jwt_metadata
  WHERE jwt_uuid = ?
  ORDER BY created_at DESC
  LIMIT 1
) m
LEFT JOIN custom_jwt.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.expires_at > now()
  AND d.jwt_uuid IS NULL;
```

## Security Features
- **UUID Primary Keys**: Prevent enumeration attacks
- **Timestamp Precision**: Support for exact expiration checking
- **Denylist Pattern**: Secure token revocation
- **PKCE Support**: Enhanced OAuth2 security
- **Separation of Concerns**: Isolated schemas for different token types

## Backup and Recovery
- Regular backups of both schemas required
- `jwt_metadata` tables contain critical token state
- Denylist tables prevent token replay attacks
- OAuth state table can be recreated (short-lived data)