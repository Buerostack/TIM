# JWT-Database Correlation

## Overview

This document explains how JWT tokens are linked to database records in TIM, providing the critical connection between the stateless JWT tokens and the persistent database storage used for tracking, revocation, and auditing.

## The Correlation Key: JWT ID (`jti`)

### What is `jti`?

**JWT ID (`jti`) Claim:**
- **Standard**: Defined in RFC 7519 (JSON Web Token standard)
- **Purpose**: Unique identifier for each JWT token
- **Format**: UUID (Universally Unique Identifier)
- **Generation**: Automatically created during JWT signing process
- **Uniqueness**: Guaranteed unique across all tokens in the system

### JWT Structure with `jti`

**Complete JWT Example:**
```json
{
  "header": {
    "kid": "jwtsign",           // Key ID for signature verification
    "alg": "RS256"              // Signature algorithm
  },
  "payload": {
    "aud": ["payment-service"],  // Audience
    "sub": "user123",           // Subject
    "role": "admin",            // Custom claim
    "department": "engineering", // Custom claim
    "permissions": ["read", "write", "delete"], // Custom claim
    "iss": "TIM",               // Issuer
    "exp": 1759095323,          // Expiration timestamp
    "iat": 1759091723,          // Issued at timestamp
    "jti": "4b2bc14c-c234-4f63-a4dd-4522860d9b36"  ← Correlation Key
  },
  "signature": "..."
}
```

**Key Points:**
- **`jti`** is the unique correlation field
- **Database tables use `jwt_uuid`** which equals the JWT's `jti`
- **One-to-one mapping** between JWT and database records

## Database Table Correlation

### Primary Correlation Pattern

**All JWT-related tables use the same correlation pattern:**

```sql
-- Custom schema tables
custom.jwt_metadata.jwt_uuid = JWT's jti claim
custom.denylist.jwt_uuid     = JWT's jti claim
custom.allowlist.jwt_hash    = Hash of JWT (different pattern)

-- TARA schema tables
tara.jwt_metadata.jwt_uuid   = JWT's jti claim
tara.denylist.jwt_uuid       = JWT's jti claim
tara.allowlist.jwt_hash      = Hash of JWT (different pattern)
```

### Correlation in Action

**1. Token Generation → Database Storage:**
```java
// Step 1: Generate JWT with automatic jti
String token = signer.sign(claims, issuer, audiences, ttl);

// Step 2: Parse JWT to extract jti
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Step 3: Store metadata using jti as primary key
var meta = new CustomJwtMetadata();
meta.setJwtUuid(jti);  // ← Correlation established
meta.setClaimKeys(String.join(",", claims.keySet()));
meta.setIssuedAt(jwt.getJWTClaimsSet().getIssueTime().toInstant());
meta.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
metaRepo.save(meta);

// Result: JWT's jti is now linked to database record
```

**2. Token Validation → Database Lookup:**
```java
// Step 1: Parse incoming JWT
var jwt = SignedJWT.parse(incomingToken);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Step 2: Check if token is revoked using jti
boolean isRevoked = denylistRepo.findById(jti).isPresent();

// Step 3: Get token metadata using jti
Optional<CustomJwtMetadata> metadata = metaRepo.findById(jti);

// Result: Database lookup using JWT's jti provides all related data
```

**3. Token Revocation → Database Update:**
```java
// Step 1: Parse JWT to revoke
var jwt = SignedJWT.parse(tokenToRevoke);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Step 2: Add to denylist using jti
var dl = new CustomDenylist();
dl.setJwtUuid(jti);  // ← Same correlation key
dl.setDenylistedAt(Instant.now());
dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
denylistRepo.save(dl);

// Result: JWT is now marked as revoked in database
```

## Correlation Across Tables

### Multi-Table Relationships

**Token Lifecycle Correlation:**
```sql
-- Find complete token information using jti
SELECT
    m.jwt_uuid,
    m.claim_keys,
    m.issued_at,
    m.expires_at,
    CASE
        WHEN d.jwt_uuid IS NOT NULL THEN 'REVOKED'
        WHEN m.expires_at < NOW() THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END as status,
    d.denylisted_at
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';
```

**Cross-Schema Independence:**
```sql
-- Custom and TARA tokens are completely separate
-- Same jti could theoretically exist in both schemas (different token ecosystems)
SELECT 'custom' as schema, COUNT(*) FROM custom.jwt_metadata
UNION ALL
SELECT 'tara' as schema, COUNT(*) FROM tara.jwt_metadata;

-- No foreign key relationships between schemas
-- No risk of confusion between custom and TARA tokens
```

## Practical Examples

### Example 1: Complete Token Lifecycle

**Step 1: Generate Token**
```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "API_TOKEN",
    "content": {"sub": "user123", "role": "admin"},
    "expirationInMinutes": 60
  }'
```

**Response:**
```json
{
  "status": "created",
  "name": "API_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "expiresAt": "2025-09-28T21:42:28Z"
}
```

**Database Record Created:**
```sql
-- Automatically created in custom.jwt_metadata
INSERT INTO custom.jwt_metadata (jwt_uuid, claim_keys, issued_at, expires_at)
VALUES (
  '57a6e593-1304-4b15-a8df-8a908a50a8ff',  -- From JWT's jti
  'sub,role',                               -- From claims
  '2025-09-28 20:42:28',                   -- From JWT's iat
  '2025-09-28 21:42:28'                    -- From JWT's exp
);
```

**Step 2: Validate Token**
```bash
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIi..."}'
```

**Validation Process:**
```java
// 1. Parse JWT and extract jti
var jti = UUID.fromString("57a6e593-1304-4b15-a8df-8a908a50a8ff");

// 2. Check revocation status
SELECT jwt_uuid FROM custom.denylist WHERE jwt_uuid = '57a6e593-1304-4b15-a8df-8a908a50a8ff';
// Result: Empty (not revoked)

// 3. Return validation success
```

**Step 3: Revoke Token**
```java
// Application revokes token
customJwtService.denylist("eyJraWQiOiJqd3RzaWduIi...");
```

**Database Record Created:**
```sql
-- Automatically created in custom.denylist
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
VALUES (
  '57a6e593-1304-4b15-a8df-8a908a50a8ff',  -- Same jti from JWT
  '2025-09-28 20:50:00',                   -- Current timestamp
  '2025-09-28 21:42:28'                    -- Original expiration
);
```

**Step 4: Re-validate Token**
```bash
# Same validation request as Step 2
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIi..."}'
```

**Validation Process:**
```java
// 1. Parse JWT and extract same jti
var jti = UUID.fromString("57a6e593-1304-4b15-a8df-8a908a50a8ff");

// 2. Check revocation status
SELECT jwt_uuid FROM custom.denylist WHERE jwt_uuid = '57a6e593-1304-4b15-a8df-8a908a50a8ff';
// Result: Found (token is revoked)

// 3. Return validation failure
```

**Response:**
```json
{
  "valid": false,
  "active": false,
  "reason": "Token revoked",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

### Example 2: Analytics Using Correlation

**Find All Data for Specific Token:**
```sql
-- Using the jti from the JWT
WITH token_jti AS (
  SELECT '57a6e593-1304-4b15-a8df-8a908a50a8ff'::uuid as jti
)
SELECT
  'metadata' as table_name,
  m.jwt_uuid::text as uuid,
  m.claim_keys as data,
  m.issued_at::text as timestamp,
  null as additional_info
FROM custom.jwt_metadata m, token_jti t
WHERE m.jwt_uuid = t.jti

UNION ALL

SELECT
  'denylist' as table_name,
  d.jwt_uuid::text as uuid,
  'revoked' as data,
  d.denylisted_at::text as timestamp,
  d.expires_at::text as additional_info
FROM custom.denylist d, token_jti t
WHERE d.jwt_uuid = t.jti;
```

**Result:**
```
table_name | uuid                                 | data     | timestamp           | additional_info
-----------|--------------------------------------|----------|---------------------|------------------
metadata   | 57a6e593-1304-4b15-a8df-8a908a50a8ff | sub,role | 2025-09-28 20:42:28 | null
denylist   | 57a6e593-1304-4b15-a8df-8a908a50a8ff | revoked  | 2025-09-28 20:50:00 | 2025-09-28 21:42:28
```

## Correlation Benefits

### 1. **Fast Validation Performance**

**Primary Key Lookups:**
```sql
-- Extremely fast O(1) lookup using primary key index
SELECT jwt_uuid FROM custom.denylist WHERE jwt_uuid = ?;

-- No need for complex queries or joins for basic validation
-- Uses automatic primary key index for maximum performance
```

### 2. **Complete Audit Trail**

**Token History:**
```sql
-- Complete lifecycle of any token using jti
SELECT
  'generated' as event,
  issued_at as event_time,
  claim_keys as details
FROM custom.jwt_metadata
WHERE jwt_uuid = ?

UNION ALL

SELECT
  'revoked' as event,
  denylisted_at as event_time,
  'manual revocation' as details
FROM custom.denylist
WHERE jwt_uuid = ?

ORDER BY event_time;
```

### 3. **Data Consistency**

**Referential Integrity:**
```sql
-- Find orphaned denylist entries (shouldn't exist)
SELECT d.jwt_uuid
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL;

-- Find tokens with metadata but check revocation status
SELECT
  m.jwt_uuid,
  CASE WHEN d.jwt_uuid IS NOT NULL THEN 'revoked' ELSE 'active' END as status
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.expires_at > NOW();
```

### 4. **Efficient Cleanup**

**Coordinated Cleanup:**
```sql
-- Clean expired tokens and related data together
DELETE FROM custom.denylist
WHERE expires_at < NOW();

DELETE FROM custom.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '30 days';  -- Keep metadata longer for audit
```

## Correlation Challenges and Solutions

### Challenge 1: JWT Parsing Overhead

**Problem**: Every validation requires JWT parsing to extract `jti`

**Current Approach:**
```java
// Parse JWT on every validation
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
```

**Optimization Opportunities:**
```java
// Cache parsed JWTs (careful with memory usage)
@Cacheable("parsed-jwts")
public ParsedJWT parseJWT(String token) {
    // Parse and cache for short period
}

// Or extract jti without full parsing (if JWT structure is predictable)
public UUID extractJTI(String token) {
    // Fast jti extraction without full validation
}
```

### Challenge 2: Database Storage Overhead

**Problem**: Every JWT generates database records

**Current Storage:**
```sql
-- Each token creates 1 metadata record
-- Revoked tokens create 1 additional denylist record
-- Storage grows linearly with token usage
```

**Optimization Strategies:**
```sql
-- Regular cleanup of expired data
DELETE FROM custom.jwt_metadata WHERE expires_at < NOW() - INTERVAL '7 days';

-- Partition tables by time for better performance
CREATE TABLE custom.jwt_metadata_2024_q1 PARTITION OF custom.jwt_metadata
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
```

### Challenge 3: Cross-Schema Token Confusion

**Problem**: Same `jti` could theoretically exist in both schemas

**Solution**: Schema isolation prevents this issue
```sql
-- Tokens are completely isolated by schema
-- No possibility of confusion between custom and TARA tokens
-- Each schema maintains independent UUID space
```

## Advanced Correlation Patterns

### Pattern 1: Batch Operations

**Bulk Token Revocation:**
```sql
-- Revoke all tokens from specific time period
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE issued_at BETWEEN '2024-01-01 09:00:00' AND '2024-01-01 10:00:00'
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);
```

### Pattern 2: Analytics Correlation

**Token Usage Analytics:**
```sql
-- Correlate token generation with claim patterns
SELECT
  claim_keys,
  COUNT(*) as tokens_generated,
  COUNT(d.jwt_uuid) as tokens_revoked,
  ROUND(COUNT(d.jwt_uuid) * 100.0 / COUNT(*), 2) as revocation_rate
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.issued_at > NOW() - INTERVAL '30 days'
GROUP BY claim_keys
ORDER BY tokens_generated DESC;
```

### Pattern 3: Security Monitoring

**Suspicious Token Activity:**
```sql
-- Find tokens that were revoked shortly after creation
SELECT
  m.jwt_uuid,
  m.issued_at,
  d.denylisted_at,
  EXTRACT(EPOCH FROM (d.denylisted_at - m.issued_at))/60 as minutes_to_revocation
FROM custom.jwt_metadata m
JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE d.denylisted_at - m.issued_at < INTERVAL '5 minutes'
  AND d.denylisted_at > NOW() - INTERVAL '24 hours'
ORDER BY minutes_to_revocation;
```

## Testing Correlation

### Verification Scripts

**Test Correlation Integrity:**
```bash
#!/bin/bash

# Generate test token
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"content": {"sub": "test"}, "expirationInMinutes": 60}' \
  | jq -r '.token')

# Extract jti from JWT (requires jwt CLI tool)
JTI=$(echo $TOKEN | jwt decode - | jq -r '.jti')

# Verify database record exists
echo "Checking database for JTI: $JTI"
docker exec tim-postgres psql -U tim -d tim -c \
  "SELECT jwt_uuid, claim_keys FROM custom.jwt_metadata WHERE jwt_uuid = '$JTI';"

# Test validation
echo "Testing validation..."
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\"}" | jq '.valid'

echo "Correlation test complete"
```

### Debugging Correlation Issues

**Common Debugging Queries:**
```sql
-- Find tokens with metadata but missing from validation
SELECT m.jwt_uuid, m.claim_keys
FROM custom.jwt_metadata m
WHERE m.expires_at > NOW()
  AND m.jwt_uuid NOT IN (
    -- Add actual validation logic here
    SELECT jwt_uuid FROM custom.denylist
  );

-- Find denylist entries without metadata (shouldn't happen)
SELECT d.jwt_uuid, d.denylisted_at
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL;

-- Verify correlation counts
SELECT
  (SELECT COUNT(*) FROM custom.jwt_metadata) as metadata_count,
  (SELECT COUNT(*) FROM custom.denylist) as denylist_count,
  (SELECT COUNT(DISTINCT jwt_uuid) FROM custom.denylist) as unique_revoked_count;
```

---

## Related Documentation

- **[Database Setup](./01-database-setup.md)**: Database initialization and configuration
- **[Schema Architecture](./02-schema-architecture.md)**: Overall database design
- **[Custom JWT Tables](./03-custom-tables.md)**: Custom schema table details
- **[TARA Tables](./04-tara-tables.md)**: TARA schema table details
- **[Database Operations](./06-operations.md)**: Common operations and maintenance