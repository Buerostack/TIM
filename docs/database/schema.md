# TIM 2.0 Database Schema

## Overview
TIM 2.0 uses PostgreSQL with two main schemas:
- **custom**: Custom JWT token management
- **tara**: TARA/OAuth2 authentication support

## Schema: custom

### Table: jwt_metadata
Stores metadata for all custom JWT tokens issued by TIM.

```sql
CREATE TABLE custom.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL,
  subject text,
  jwt_name text,
  audience text,
  issuer text
);
```

**Indexes:**
- `idx_custom_jwt_metadata_subject` on `subject` - Fast lookups by user
- `idx_custom_jwt_metadata_issued` on `issued_at` - Chronological ordering

**Fields:**
- `jwt_uuid`: Unique identifier matching JWT `jti` claim
- `claim_keys`: JSON serialized custom claims
- `issued_at`: Token creation timestamp
- `expires_at`: Token expiration timestamp
- `subject`: User identifier (`sub` claim)
- `jwt_name`: Human-readable token name
- `audience`: Target audience (`aud` claim)
- `issuer`: Token issuer (`iss` claim)

### Table: denylist
Tracks revoked JWT tokens to prevent reuse.

```sql
CREATE TABLE custom.denylist (
  jwt_uuid uuid PRIMARY KEY,
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);
```

**Indexes:**
- `idx_custom_denylist_exp` on `expires_at` - Cleanup expired entries

**Fields:**
- `jwt_uuid`: References `jwt_metadata.jwt_uuid`
- `denylisted_at`: Revocation timestamp
- `expires_at`: Original token expiration (for cleanup)
- `reason`: Optional revocation reason

## Schema: tara

### Table: jwt_metadata
Simplified metadata for TARA OAuth2 tokens.

```sql
CREATE TABLE tara.jwt_metadata (
  jwt_uuid uuid PRIMARY KEY,
  claim_keys text NOT NULL,
  issued_at timestamp NOT NULL,
  expires_at timestamp NOT NULL
);
```

**Fields:**
- `jwt_uuid`: Unique token identifier
- `claim_keys`: Serialized token claims
- `issued_at`: Token creation timestamp
- `expires_at`: Token expiration timestamp

### Table: denylist
Revoked TARA tokens tracking.

```sql
CREATE TABLE tara.denylist (
  jwt_uuid uuid PRIMARY KEY,
  denylisted_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  reason text
);
```

**Indexes:**
- `idx_tara_denylist_exp` on `expires_at` - Cleanup expired entries

### Table: oauth_state
OAuth2 state management for PKCE flows.

```sql
CREATE TABLE tara.oauth_state (
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
1. **Generation**: Insert into `custom.jwt_metadata`
2. **Usage**: Validate against `custom.denylist`
3. **Extension**: Update `expires_at` in `custom.jwt_metadata`
4. **Revocation**: Insert into `custom.denylist`
5. **Cleanup**: Remove expired entries from both tables

### OAuth2/TARA Flow
1. **Login Initiation**: Insert state into `tara.oauth_state`
2. **Callback**: Validate state, insert token into `tara.jwt_metadata`
3. **Token Usage**: Check against `tara.denylist`
4. **Logout**: Insert into `tara.denylist`

## Performance Considerations

### Indexes
- Subject-based queries use `idx_custom_jwt_metadata_subject`
- Chronological listing uses `idx_custom_jwt_metadata_issued`
- Expiration cleanup uses denylist expiration indexes

### Cleanup Strategy
```sql
-- Clean expired denylist entries
DELETE FROM custom.denylist WHERE expires_at < now();
DELETE FROM tara.denylist WHERE expires_at < now();

-- Clean expired OAuth states (recommend 1 hour TTL)
DELETE FROM tara.oauth_state WHERE created_at < now() - interval '1 hour';
```

### Query Patterns
```sql
-- Find user's active tokens
SELECT * FROM custom.jwt_metadata
WHERE subject = ?
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist)
  AND expires_at > now()
ORDER BY issued_at DESC;

-- Validate token
SELECT 1 FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.jwt_uuid = ?
  AND m.expires_at > now()
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