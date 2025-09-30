# JWT Extension Chain Architecture

## Overview

TIM 2.0 implements a comprehensive JWT extension chain system that provides complete audit trails for token lifecycle management. This document describes the architecture, data model, and operational characteristics of the extension chain functionality.

## Core Concepts

### Extension Chain
An extension chain is a chronological sequence of JWT tokens where each new token extends the lifetime of the previous one while maintaining a complete audit trail.

```
Original JWT → Extension 1 → Extension 2 → Extension 3
     ↓              ↓              ↓              ↓
   Active         Revoked        Revoked      Active (Current)
```

### Key Principles

1. **INSERT-Only Operations**: No UPDATE queries are used; each extension creates a new database record
2. **Complete Audit Trail**: Every extension is permanently recorded with timestamps and relationships
3. **Immutable History**: Once created, extension chain records cannot be modified
4. **Automatic Revocation**: Previous tokens are automatically revoked when extended

## Data Model

### Database Schema

```sql
CREATE TABLE custom_jwt.jwt_metadata (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),     -- Unique record identifier
  jwt_uuid uuid NOT NULL,                            -- JWT identifier (jti claim)
  created_at timestamp NOT NULL DEFAULT now(),       -- Database insertion time
  claim_keys text NOT NULL,                          -- Comma-separated claim names
  issued_at timestamp NOT NULL,                      -- JWT iat claim
  expires_at timestamp NOT NULL,                     -- JWT exp claim
  subject text,                                      -- JWT sub claim
  jwt_name text,                                     -- Human-readable token name
  audience text,                                     -- JWT aud claim (comma-separated)
  issuer text,                                       -- JWT iss claim
  supersedes uuid,                                   -- Previous version's ID
  original_jwt_uuid uuid NOT NULL                    -- First JWT in the chain
);
```

### Key Fields

| Field | Purpose | Example |
|-------|---------|---------|
| `id` | Unique database record identifier | `550e8400-e29b-41d4-a716-446655440000` |
| `jwt_uuid` | JWT identifier from `jti` claim | `12345678-1234-1234-1234-123456789012` |
| `created_at` | When this record was created in DB | `2024-01-15T10:30:00Z` |
| `supersedes` | Previous version's `id` (NULL for original) | `550e8400-e29b-41d4-a716-446655440001` |
| `original_jwt_uuid` | First JWT's `jwt_uuid` in chain | `12345678-1234-1234-1234-123456789012` |

### Indexes for Performance

```sql
-- Critical for finding current version
CREATE INDEX idx_custom_jwt_metadata_jwt_uuid ON custom_jwt.jwt_metadata (jwt_uuid, created_at DESC);

-- Critical for extension chain queries
CREATE INDEX idx_custom_jwt_metadata_original ON custom_jwt.jwt_metadata (original_jwt_uuid);

-- For user token queries
CREATE INDEX idx_custom_jwt_metadata_subject ON custom_jwt.jwt_metadata (subject);

-- For chronological queries
CREATE INDEX idx_custom_jwt_metadata_issued ON custom_jwt.jwt_metadata (issued_at);
```

## Extension Chain Operations

### 1. New JWT Generation

When generating a new JWT:

```java
// Create new JWT
String newToken = jwtSignerService.sign(claims, issuer, audiences, ttl);
SignedJWT jwt = SignedJWT.parse(newToken);
UUID jwtUuid = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Create metadata record
CustomJwtMetadata metadata = new CustomJwtMetadata(
    jwtUuid,                    // jwt_uuid
    "sub,role,iat,exp",        // claim_keys
    jwt.getIssueTime(),        // issued_at
    jwt.getExpirationTime(),   // expires_at
    jwtUuid                    // original_jwt_uuid = jwt_uuid for new tokens
);
// supersedes remains NULL for new tokens
metadataRepo.save(metadata);
```

**Result**: Single record in database representing the original JWT.

### 2. JWT Extension

When extending an existing JWT:

```java
// Parse old token
SignedJWT oldJwt = SignedJWT.parse(oldToken);
UUID oldJwtUuid = UUID.fromString(oldJwt.getJWTClaimsSet().getJWTID());

// Find current version
CustomJwtMetadata currentVersion = metadataRepo
    .findCurrentVersionByJwtUuid(oldJwtUuid)
    .orElseThrow(() -> new Exception("JWT metadata not found"));

// Generate new token with same claims
String newToken = jwtSignerService.sign(preservedClaims, issuer, audiences, ttl);
SignedJWT newJwt = SignedJWT.parse(newToken);
UUID newJwtUuid = UUID.fromString(newJwt.getJWTClaimsSet().getJWTID());

// Create extension record
CustomJwtMetadata extension = new CustomJwtMetadata(
    newJwtUuid,                           // New JWT UUID
    currentVersion.getClaimKeys(),        // Same claim structure
    newJwt.getIssueTime(),               // New issued time
    newJwt.getExpirationTime(),          // Extended expiration
    currentVersion.getOriginalJwtUuid()   // Points to original
);
extension.setSupersedes(currentVersion.getId());  // Links to previous version
metadataRepo.save(extension);

// Revoke old token
denylistService.revoke(oldToken);
```

**Result**: New record created, old token revoked, extension chain preserved.

## Query Patterns

### Find Current Version of JWT

```sql
SELECT * FROM custom_jwt.jwt_metadata
WHERE jwt_uuid = ?
ORDER BY created_at DESC
LIMIT 1;
```

**Use Case**: Token validation, introspection

### Find Complete Extension Chain

```sql
SELECT * FROM custom_jwt.jwt_metadata
WHERE original_jwt_uuid = ?
ORDER BY created_at ASC;
```

**Use Case**: Audit trails, debugging, analytics

### Find Active JWTs for User

```sql
SELECT DISTINCT ON (jwt_uuid) *
FROM custom_jwt.jwt_metadata
WHERE subject = ?
ORDER BY jwt_uuid, created_at DESC;
```

**Use Case**: User token management, dashboard

### Find Extension Count

```sql
SELECT COUNT(*) - 1 as extension_count
FROM custom_jwt.jwt_metadata
WHERE original_jwt_uuid = ?;
```

**Use Case**: Analytics, introspection response

## Extension Chain Examples

### Example 1: Simple Extension

```
Time: T0 - Original JWT Created
┌─────────────────────────────────────────────────────────────┐
│ ID: meta-001                                                │
│ jwt_uuid: jwt-001                                          │
│ created_at: T0                                             │
│ supersedes: NULL                                           │
│ original_jwt_uuid: jwt-001                                 │
│ expires_at: T0 + 1h                                       │
└─────────────────────────────────────────────────────────────┘

Time: T0 + 50min - JWT Extended
┌─────────────────────────────────────────────────────────────┐
│ ID: meta-002                                                │
│ jwt_uuid: jwt-002                                          │
│ created_at: T0 + 50min                                     │
│ supersedes: meta-001                                       │
│ original_jwt_uuid: jwt-001                                 │
│ expires_at: T0 + 50min + 1h                               │
└─────────────────────────────────────────────────────────────┘
```

### Example 2: Multiple Extensions

```
Original → Extension 1 → Extension 2 → Extension 3

jwt-001     jwt-002      jwt-003      jwt-004
  ↓           ↓            ↓            ↓
meta-001 → meta-002 → meta-003 → meta-004
(NULL)    (meta-001)  (meta-002)  (meta-003)
  ↓           ↓            ↓            ↓
REVOKED    REVOKED      REVOKED      ACTIVE

All point to original_jwt_uuid: jwt-001
```

## Security Considerations

### Revocation Cascade

When a JWT is extended:
1. New JWT is created with new UUID
2. Previous JWT is immediately revoked (added to denylist)
3. Extension chain relationship is recorded
4. Original JWT UUID is preserved for audit trail

### Audit Trail Protection

- Extension chain records are immutable once created
- Database-level constraints prevent modification of `created_at`
- Complete chronological history is always available
- Supersession relationships cannot be altered

### Performance Security

- Indexes prevent denial-of-service through slow queries
- Extension chain depth can be monitored and limited
- Cleanup processes remove expired chains automatically

## Operational Characteristics

### Performance Metrics

| Operation | Typical Performance | Index Dependency |
|-----------|-------------------|------------------|
| Find current version | < 1ms | `idx_jwt_uuid` |
| Find extension chain | < 5ms | `idx_original` |
| Create extension | < 10ms | Primary key |
| User token lookup | < 3ms | `idx_subject` |

### Storage Characteristics

- **Growth Rate**: Linear with extension frequency
- **Retention**: Based on cleanup policies for expired chains
- **Compression**: Historical data can be archived/compressed

### Cleanup Strategy

```sql
-- Clean expired extension chains (original JWT expired)
DELETE FROM custom_jwt.jwt_metadata
WHERE original_jwt_uuid IN (
    SELECT original_jwt_uuid
    FROM custom_jwt.jwt_metadata
    WHERE created_at = (
        SELECT MIN(created_at)
        FROM custom_jwt.jwt_metadata m2
        WHERE m2.original_jwt_uuid = jwt_metadata.original_jwt_uuid
    )
    AND expires_at < NOW() - INTERVAL '30 days'  -- Grace period
);
```

## Integration Points

### Token Introspection

Extension chain information is included in RFC 7662 introspection responses:

```json
{
  "active": true,
  "jwt_name": "USER_TOKEN",
  "original_jwt_uuid": "12345678-1234-1234-1234-123456789012",
  "extension_count": 2,
  "supersedes": "87654321-4321-4321-4321-210987654321",
  "created_at": 1640905200
}
```

### Monitoring and Analytics

Extension chain data enables:
- Token usage pattern analysis
- Extension frequency monitoring
- Security audit trail generation
- Performance optimization insights

### API Endpoints

- `GET /jwt/custom/extension-chain/{originalJwtUuid}` - Retrieve complete chain
- `POST /introspect` - Include extension chain info in response
- `POST /jwt/custom/extend` - Create new extension

## Migration Considerations

### From Legacy Systems

When migrating from systems without extension chains:
1. Existing JWTs become "original" tokens
2. Set `original_jwt_uuid = jwt_uuid` for existing records
3. Set `supersedes = NULL` for all existing records
4. Extensions created after migration follow new pattern

### Schema Updates

Extension chain implementation requires:
- New columns: `supersedes`, `original_jwt_uuid`, `created_at`
- New indexes for performance
- Updated queries in application code
- Migration scripts for existing data

## Best Practices

### Development Guidelines

1. **Always use repository methods** for extension chain queries
2. **Never UPDATE** metadata records; always INSERT
3. **Validate chain integrity** in tests
4. **Monitor extension frequency** for abuse detection
5. **Include extension info** in relevant API responses

### Operational Guidelines

1. **Monitor query performance** with extension chain indexes
2. **Set up cleanup jobs** for expired chains
3. **Track extension patterns** for security analysis
4. **Backup extension chain data** for audit requirements
5. **Test disaster recovery** with chain data integrity