# Custom JWT Tables

## Overview

The `custom` schema contains tables specifically designed for TIM's custom JWT functionality. These tables handle token generation, tracking, and revocation for API authentication and service-to-service communication.

## Table Structure

### `custom.jwt_metadata`

**Purpose**: Comprehensive tracking and auditing of all custom JWT tokens issued by TIM.

#### Schema Definition
```sql
CREATE TABLE custom.jwt_metadata (
    jwt_uuid   UUID PRIMARY KEY,                       -- JWT's 'jti' claim (unique identifier)
    claim_keys TEXT NOT NULL,                          -- Comma-separated custom claim names
    issued_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,   -- Token issuance timestamp
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL    -- Token expiration timestamp
);

-- Primary key index (automatic)
-- jwt_metadata_pkey PRIMARY KEY, btree (jwt_uuid)
```

#### Field Details

**`jwt_uuid` (UUID, Primary Key)**
- **Source**: Extracted from JWT's `jti` (JWT ID) claim
- **Purpose**: Unique identifier linking JWT to database record
- **Generation**: Automatically created during JWT signing process
- **Format**: Standard UUID v4 (e.g., `4b2bc14c-c234-4f63-a4dd-4522860d9b36`)

**`claim_keys` (TEXT, NOT NULL)**
- **Content**: Comma-separated list of custom claim names
- **Purpose**: Track which claims were included in the token
- **Examples**:
  - `"sub,role"` - Basic user token
  - `"sub,role,department,permissions"` - Extended claims
  - `"service,scope,permissions"` - Service token
- **Analytics**: Enables claim usage pattern analysis

**`issued_at` (TIMESTAMP, NOT NULL)**
- **Source**: JWT's `iat` (issued at) claim
- **Purpose**: Audit trail and analytics
- **Format**: UTC timestamp without timezone
- **Use Cases**: Token age analysis, security monitoring

**`expires_at` (TIMESTAMP, NOT NULL)**
- **Source**: JWT's `exp` (expiration) claim
- **Purpose**: Cleanup automation and validation
- **Format**: UTC timestamp without timezone
- **Use Cases**: Automated cleanup, expiration monitoring

#### Usage Patterns

**Token Generation Flow:**
```java
// CustomJwtService.generate()
String token = signer.sign(claims, issuer, audiences, ttl);
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Create metadata record
var meta = new CustomJwtMetadata();
meta.setJwtUuid(jti);                                    // Links to JWT
meta.setIssuedAt(jwt.getJWTClaimsSet().getIssueTime().toInstant());
meta.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
meta.setClaimKeys(String.join(",", claims.keySet()));    // Track claim usage
metaRepo.save(meta);
```

**Analytics Queries:**
```sql
-- Most popular claim combinations
SELECT claim_keys, COUNT(*) as usage_count
FROM custom.jwt_metadata
GROUP BY claim_keys
ORDER BY COUNT(*) DESC;

-- Token generation rate over time
SELECT DATE(issued_at), COUNT(*) as tokens_issued
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(issued_at)
ORDER BY DATE(issued_at);

-- Claim usage trends
SELECT
    claim_keys,
    DATE(issued_at) as issue_date,
    COUNT(*) as daily_count
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '7 days'
GROUP BY claim_keys, DATE(issued_at)
ORDER BY issue_date, daily_count DESC;
```

### `custom.denylist`

**Purpose**: Manage revoked JWT tokens to prevent their use after revocation.

#### Schema Definition
```sql
CREATE TABLE custom.denylist (
    jwt_uuid      UUID PRIMARY KEY,                     -- Links to jwt_metadata.jwt_uuid
    denylisted_at TIMESTAMP NOT NULL DEFAULT now(),     -- Revocation timestamp
    expires_at    TIMESTAMP NOT NULL,                   -- Original expiration (for cleanup)\n    reason        TEXT                                  -- Optional revocation reason (audit)
);

-- Indexes
-- denylist_pkey PRIMARY KEY, btree (jwt_uuid)
-- idx_custom_denylist_exp btree (expires_at)  -- Cleanup optimization
```

#### Field Details

**`jwt_uuid` (UUID, Primary Key)**
- **Relationship**: Foreign key to `custom.jwt_metadata.jwt_uuid`
- **Purpose**: Links revocation to specific token
- **Uniqueness**: Each token can only be revoked once
- **Validation Use**: Fast lookup during token validation

**`denylisted_at` (TIMESTAMP, NOT NULL, DEFAULT now())**
- **Purpose**: Audit trail for revocation events
- **Default**: Automatically set to current timestamp
- **Use Cases**: Security incident analysis, revocation monitoring

**`expires_at` (TIMESTAMP, NOT NULL)**
- **Source**: Copied from original JWT's expiration
- **Purpose**: Enables cleanup of expired denylist entries
- **Optimization**: Prevents denylist from growing indefinitely
- **Cleanup**: Automated removal after token would have expired naturally\n\n**`reason` (TEXT, NULLABLE)**\n- **Purpose**: Optional audit trail for revocation reasoning\n- **Examples**: \"user_logout\", \"security_incident\", \"admin_action\"\n- **Use Cases**: Compliance reporting, security analysis, incident investigation\n- **Storage**: Free-form text field for maximum flexibility

#### Usage Patterns

**Token Revocation:**
```java
// CustomJwtService.denylist()
public void denylist(String token, String reason) throws Exception {
    var jwt = SignedJWT.parse(token);
    var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

    var dl = new CustomDenylist();
    dl.setJwtUuid(jti);                                      // Link to token
    dl.setDenylistedAt(Instant.now());                      // Revocation time
    dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
    dl.setReason(reason);                                    // Audit reason
    denylistRepo.save(dl);
}
```

**Validation Check:**
```java
// CustomJwtService.isRevoked()
public boolean isRevoked(String token) {
    try {
        var jwt = SignedJWT.parse(token);
        var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
        return denylistRepo.findById(jti).isPresent();      // Fast primary key lookup
    } catch (Exception e) {
        return true;  // Invalid tokens are considered revoked
    }
}
```

**Cleanup Operations:**
```sql
-- Remove expired denylist entries (automated cleanup)
DELETE FROM custom.denylist
WHERE expires_at < NOW();

-- Security monitoring: recent revocations
SELECT jwt_uuid, denylisted_at, expires_at
FROM custom.denylist
WHERE denylisted_at > NOW() - INTERVAL '24 hours'
ORDER BY denylisted_at DESC;

-- Mass revocation (security incident response)
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE issued_at BETWEEN '2024-01-01 10:00:00' AND '2024-01-01 11:00:00'
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);
```

## Table Relationships

### Primary Relationships

**JWT Metadata ←→ Denylist:**
```sql
-- One-to-zero-or-one relationship
custom.jwt_metadata.jwt_uuid ←→ custom.denylist.jwt_uuid

-- Find metadata for revoked tokens
SELECT m.*, d.denylisted_at
FROM custom.jwt_metadata m
JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid;

-- Find active (non-revoked) tokens
SELECT m.*
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE d.jwt_uuid IS NULL
  AND m.expires_at > NOW();
```

### Data Integrity

**Referential Integrity Considerations:**
- No formal foreign key constraints (for performance)
- Application-level integrity enforcement
- Orphaned record cleanup through scheduled jobs

**Cleanup Coordination:**
```sql
-- Coordinated cleanup: remove metadata and related denylist entries
-- Step 1: Clean expired denylist entries
DELETE FROM custom.denylist WHERE expires_at < NOW();

-- Step 2: Clean expired metadata (keep for audit period)
DELETE FROM custom.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '30 days';

-- Step 3: Clean orphaned denylist entries (if any)
DELETE FROM custom.denylist d
WHERE NOT EXISTS (
    SELECT 1 FROM custom.jwt_metadata m
    WHERE m.jwt_uuid = d.jwt_uuid
);
```

## Performance Optimization

### Current Index Strategy

**Primary Key Indexes (Automatic):**
```sql
-- Fast UUID lookups for validation
jwt_metadata_pkey (jwt_uuid)
denylist_pkey (jwt_uuid)
```

**Cleanup Optimization Indexes:**
```sql
-- Efficient expiration-based cleanup
idx_custom_denylist_exp (expires_at)
```

### Missing Indexes (Performance Opportunities)

**Time-Based Queries:**
```sql
-- For analytics and monitoring
CREATE INDEX idx_custom_jwt_metadata_issued ON custom.jwt_metadata (issued_at);
CREATE INDEX idx_custom_jwt_metadata_expires ON custom.jwt_metadata (expires_at);

-- For revocation monitoring
CREATE INDEX idx_custom_denylist_denylisted ON custom.denylist (denylisted_at);
```

**Claim-Based Searches:**
```sql
-- For claim usage analytics
CREATE INDEX idx_custom_jwt_metadata_claims ON custom.jwt_metadata (claim_keys);

-- For full-text search on claims (if needed)
CREATE INDEX idx_custom_jwt_metadata_claims_fts
ON custom.jwt_metadata
USING gin (to_tsvector('english', claim_keys));
```

### Query Performance Tips

**Efficient Validation Queries:**
```sql
-- Good: Uses primary key index
SELECT 1 FROM custom.denylist WHERE jwt_uuid = ?;

-- Good: Uses expiration index
SELECT COUNT(*) FROM custom.jwt_metadata WHERE expires_at < NOW();

-- Avoid: Full table scan
SELECT * FROM custom.jwt_metadata WHERE claim_keys LIKE '%role%';

-- Better: Use specific patterns
SELECT * FROM custom.jwt_metadata WHERE claim_keys = 'sub,role,department';
```

## Monitoring and Analytics

### Key Metrics

**Token Generation Metrics:**
```sql
-- Tokens issued per day
SELECT DATE(issued_at), COUNT(*) as tokens_per_day
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(issued_at);

-- Average token lifetime
SELECT AVG(EXTRACT(EPOCH FROM (expires_at - issued_at))/3600) as avg_lifetime_hours
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '7 days';

-- Claim complexity distribution
SELECT
    ARRAY_LENGTH(STRING_TO_ARRAY(claim_keys, ','), 1) as claim_count,
    COUNT(*) as token_count
FROM custom.jwt_metadata
GROUP BY ARRAY_LENGTH(STRING_TO_ARRAY(claim_keys, ','), 1)
ORDER BY claim_count;
```

**Security Metrics:**
```sql
-- Revocation rate
SELECT
    DATE(d.denylisted_at) as revocation_date,
    COUNT(*) as revocations,
    ROUND(COUNT(*) * 100.0 / (
        SELECT COUNT(*) FROM custom.jwt_metadata m
        WHERE DATE(m.issued_at) = DATE(d.denylisted_at)
    ), 2) as revocation_percentage
FROM custom.denylist d
WHERE d.denylisted_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(d.denylisted_at);

-- Time to revocation analysis
SELECT
    EXTRACT(EPOCH FROM (d.denylisted_at - m.issued_at))/3600 as hours_to_revocation,
    COUNT(*) as token_count
FROM custom.denylist d
JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE d.denylisted_at > NOW() - INTERVAL '30 days'
GROUP BY EXTRACT(EPOCH FROM (d.denylisted_at - m.issued_at))/3600
ORDER BY hours_to_revocation;
```

### Health Checks

**Data Consistency Checks:**
```sql
-- Orphaned denylist entries
SELECT COUNT(*) as orphaned_denylisted_tokens
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL;

-- Future-dated tokens (potential clock skew)
SELECT COUNT(*) as future_tokens
FROM custom.jwt_metadata
WHERE issued_at > NOW() + INTERVAL '5 minutes';

-- Expired but not cleaned up
SELECT COUNT(*) as expired_uncleaned
FROM custom.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '7 days';
```

## Maintenance Procedures

### Automated Cleanup

**Daily Cleanup Script:**
```sql
-- Clean expired denylist entries
DELETE FROM custom.denylist
WHERE expires_at < NOW();

-- Clean old metadata (keep 30 days for audit)
DELETE FROM custom.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '30 days';

-- Update statistics
ANALYZE custom.jwt_metadata;
ANALYZE custom.denylist;
```

**Weekly Maintenance:**
```sql
-- Vacuum tables for space reclamation
VACUUM ANALYZE custom.jwt_metadata;
VACUUM ANALYZE custom.denylist;

-- Check table sizes
SELECT
    'custom.jwt_metadata' as table_name,
    pg_size_pretty(pg_total_relation_size('custom.jwt_metadata')) as size,
    COUNT(*) as row_count
FROM custom.jwt_metadata
UNION ALL
SELECT
    'custom.denylist',
    pg_size_pretty(pg_total_relation_size('custom.denylist')),
    COUNT(*)
FROM custom.denylist;
```

### Manual Operations

**Emergency Token Revocation:**
```sql
-- Revoke all tokens from specific time range
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE issued_at BETWEEN ? AND ?
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);

-- Revoke tokens with specific claims
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE claim_keys LIKE '%admin%'
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);
```

## Integration with Application Code

### Entity Classes

**CustomJwtMetadata Entity:**
```java
@Entity
@Table(name = "jwt_metadata", schema = "custom")
public class CustomJwtMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID jwtUuid;

    @Column(name = "claim_keys", nullable = false)
    private String claimKeys;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Constructors, getters, setters...
}
```

### Repository Interfaces

**JPA Repository:**
```java
@Repository
public interface CustomJwtMetadataRepo extends JpaRepository<CustomJwtMetadata, UUID> {
    List<CustomJwtMetadata> findByExpiresAtBefore(Instant cutoff);
    List<CustomJwtMetadata> findByIssuedAtBetween(Instant start, Instant end);

    @Query("SELECT c.claimKeys, COUNT(c) FROM CustomJwtMetadata c GROUP BY c.claimKeys")
    List<Object[]> getClaimUsageStatistics();
}

@Repository
public interface CustomDenylistRepo extends JpaRepository<CustomDenylist, UUID> {
    List<CustomDenylist> findByDenylistedAtAfter(Instant cutoff);
    void deleteByExpiresAtBefore(Instant cutoff);
}
```

---

## Related Documentation

- **[Schema Architecture](./02-schema-architecture.md)**: Overall database design
- **[TARA Tables](./04-tara-tables.md)**: TARA schema comparison
- **[JWT-Database Correlation](./05-jwt-correlation.md)**: Token linking mechanisms
- **[Database Operations](./06-operations.md)**: Common queries and maintenance