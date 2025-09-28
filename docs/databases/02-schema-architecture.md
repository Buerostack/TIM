# Database Schema Architecture

## Overview

TIM's database architecture uses a dual-schema design to separate custom JWT functionality from TARA OAuth integration. This separation ensures security isolation, compliance requirements, and maintainability while providing identical operational patterns for both token ecosystems.

## Schema Structure

### Database Organization

```
tim (PostgreSQL Database)
├── custom (Schema) - Custom JWT Management
│   ├── jwt_metadata    - Token tracking and audit trail
│   ├── denylist        - Revoked token management
│   └── allowlist       - Explicitly allowed tokens
└── tara (Schema) - TARA OAuth Integration
    ├── jwt_metadata    - TARA token tracking
    ├── denylist        - TARA token revocation
    ├── allowlist       - TARA token allowlist
    └── oauth_state     - OAuth PKCE state management
```

### Schema Separation Rationale

**Why Two Identical Schemas Instead of One?**

#### 1. **Functional Isolation**

**Custom Schema Purpose:**
- **Developer-Controlled**: TIM application generates and manages tokens
- **API Authentication**: Service-to-service communication
- **Flexible Claims**: Custom business logic claims
- **Internal Use**: Company/organization internal systems

**TARA Schema Purpose:**
- **Government Integration**: Estonian e-ID and e-Residency
- **Citizen Authentication**: Public sector services
- **Compliance**: Government security standards
- **External Identity**: Third-party identity provider

#### 2. **Security Benefits**

**Data Segregation:**
```sql
-- Custom tokens cannot access TARA data
SELECT * FROM custom.jwt_metadata;  -- Only custom tokens
SELECT * FROM tara.jwt_metadata;    -- Only TARA tokens

-- No cross-schema foreign keys or dependencies
-- No risk of token confusion or misuse
```

**Access Control:**
- Different applications can have schema-specific permissions
- TARA integration can be disabled without affecting custom tokens
- Security audits can focus on specific token types

#### 3. **Compliance Requirements**

**TARA Integration Standards:**
- **Government Regulations**: Estonian e-governance requirements
- **Data Protection**: GDPR compliance for citizen data
- **Audit Trails**: Separate logging for government tokens
- **Certification**: Meeting public sector security standards

**Custom Token Flexibility:**
- **Business Logic**: Custom claims for internal processes
- **Performance**: Optimized for application-specific needs
- **Development**: Rapid iteration without compliance constraints

#### 4. **Operational Independence**

**Separate Maintenance:**
```sql
-- Clean only custom tokens
DELETE FROM custom.jwt_metadata WHERE expires_at < NOW() - INTERVAL '7 days';

-- Clean only TARA tokens (different retention policy)
DELETE FROM tara.jwt_metadata WHERE expires_at < NOW() - INTERVAL '30 days';
```

**Independent Scaling:**
- Different query patterns and performance requirements
- Separate indexing strategies
- Schema-specific optimizations

## Table Design Patterns

### Identical Structure, Different Purpose

All tables follow the same pattern but serve different token ecosystems:

#### JWT Metadata Pattern
```sql
-- Both schemas implement identical structure
CREATE TABLE {schema}.jwt_metadata (
    jwt_uuid   UUID PRIMARY KEY,           -- JWT's 'jti' claim
    claim_keys TEXT NOT NULL,              -- Comma-separated claim names
    issued_at  TIMESTAMP NOT NULL,         -- Token creation time
    expires_at TIMESTAMP NOT NULL          -- Token expiration time
);
```

**Why Identical?**
1. **Consistent Operations**: Same code patterns for both token types
2. **Shared Logic**: JWT validation follows identical patterns
3. **Maintenance**: Single codebase handles both schemas
4. **Learning Curve**: Developers understand both systems easily

#### Denylist Pattern
```sql
-- Token revocation management
CREATE TABLE {schema}.denylist (
    jwt_uuid      UUID PRIMARY KEY,       -- Links to jwt_metadata
    denylisted_at TIMESTAMP NOT NULL DEFAULT now(),  -- Revocation time
    expires_at    TIMESTAMP NOT NULL      -- Original expiration for cleanup
);
```

#### Allowlist Pattern
```sql
-- Explicit token approval (optional feature)
CREATE TABLE {schema}.allowlist (
    jwt_hash   TEXT PRIMARY KEY,          -- Hash of token content
    expires_at TIMESTAMP NOT NULL         -- Expiration for cleanup
);
```

### TARA-Specific Extensions

**OAuth State Management:**
```sql
-- Only in TARA schema - OAuth PKCE flow
CREATE TABLE tara.oauth_state (
    state         TEXT PRIMARY KEY,       -- OAuth state parameter
    created_at    TIMESTAMP NOT NULL DEFAULT now(),  -- Creation time
    pkce_verifier TEXT                    -- PKCE code verifier
);
```

**Why TARA-Only?**
- Custom JWT generation doesn't use OAuth flows
- PKCE (Proof Key for Code Exchange) is specific to OAuth 2.0
- State parameter prevents CSRF attacks in OAuth flows

## Application Integration

### Entity Mapping

**Custom Schema Entities:**
```java
@Entity
@Table(name = "jwt_metadata", schema = "custom")
public class CustomJwtMetadata {
    @Id private UUID jwtUuid;
    private String claimKeys;
    private Instant issuedAt;
    private Instant expiresAt;
}

@Entity
@Table(name = "denylist", schema = "custom")
public class CustomDenylist {
    @Id private UUID jwtUuid;
    private Instant denylistedAt;
    private Instant expiresAt;
}
```

**TARA Schema Entities:**
```java
@Entity
@Table(name = "jwt_metadata", schema = "tara")
public class TaraJwtMetadata {
    @Id private UUID jwtUuid;
    private String claimKeys;
    private Instant issuedAt;
    private Instant expiresAt;
}

@Entity
@Table(name = "oauth_state", schema = "tara")
public class TaraOauthState {
    @Id private String state;
    private Instant createdAt;
    private String pkceVerifier;
}
```

### Repository Separation

**Custom Repositories:**
```java
@Repository
public interface CustomJwtMetadataRepo extends JpaRepository<CustomJwtMetadata, UUID> {
    List<CustomJwtMetadata> findByExpiresAtBefore(Instant cutoff);
}

@Repository
public interface CustomDenylistRepo extends JpaRepository<CustomDenylist, UUID> {
    // Revocation operations for custom tokens
}
```

**TARA Repositories:**
```java
@Repository
public interface TaraJwtMetadataRepo extends JpaRepository<TaraJwtMetadata, UUID> {
    List<TaraJwtMetadata> findByExpiresAtBefore(Instant cutoff);
}

@Repository
public interface TaraOauthStateRepo extends JpaRepository<TaraOauthState, String> {
    void deleteByCreatedAtBefore(Instant cutoff);
}
```

## Data Flow Patterns

### Token Lifecycle - Custom Tokens

**1. Generation:**
```java
// CustomJwtService.generate()
String token = signer.sign(claims, issuer, audiences, ttl);
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Store in custom schema
var meta = new CustomJwtMetadata();
meta.setJwtUuid(jti);
meta.setClaimKeys(String.join(",", claims.keySet()));
customMetaRepo.save(meta);
```

**2. Validation:**
```java
// CustomJwtService.validate()
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Check revocation in custom schema
boolean revoked = customDenylistRepo.findById(jti).isPresent();
```

**3. Revocation:**
```java
// CustomJwtService.denylist()
var jwt = SignedJWT.parse(token);
var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

// Add to custom denylist
var dl = new CustomDenylist();
dl.setJwtUuid(jti);
customDenylistRepo.save(dl);
```

### Token Lifecycle - TARA Tokens

**1. OAuth Flow:**
```java
// TaraController handles OAuth callbacks
// OAuth state stored in tara.oauth_state
var state = new TaraOauthState();
state.setState(randomState);
state.setPkceVerifier(pkceVerifier);
taraOauthRepo.save(state);
```

**2. Token Storage:**
```java
// TARA tokens stored in tara schema
var meta = new TaraJwtMetadata();
meta.setJwtUuid(taraTokenJti);
taraMetaRepo.save(meta);
```

## Schema Evolution Strategy

### Current State: No Migration Framework

**Advantages:**
- **Simplicity**: Single initialization script
- **Predictability**: No automatic schema changes
- **Control**: Manual approval for all changes

**Limitations:**
- **Manual Process**: Schema changes require careful coordination
- **No Versioning**: No automatic tracking of schema versions
- **Rollback Complexity**: Manual rollback procedures

### Future Migration Considerations

**Potential Migration Framework:**
```sql
-- Versioned migration example
-- V1.0__initial_schema.sql
-- V1.1__add_indexes.sql
-- V2.0__add_tara_extensions.sql
```

**Schema Versioning Strategy:**
1. **Backward Compatibility**: New columns with defaults
2. **Schema Separation**: Independent versioning per schema
3. **Blue-Green Deployments**: Zero-downtime updates
4. **Rollback Plans**: Documented rollback procedures

## Performance Considerations

### Index Strategy

**Current Indexes:**
```sql
-- Primary keys (automatic)
jwt_metadata_pkey (jwt_uuid)
denylist_pkey (jwt_uuid)
allowlist_pkey (jwt_hash)
oauth_state_pkey (state)

-- Cleanup optimization
idx_custom_denylist_exp (expires_at)
idx_custom_allowlist_exp (expires_at)
idx_tara_denylist_exp (expires_at)
idx_tara_allowlist_exp (expires_at)
```

**Missing Indexes (Performance Opportunities):**
```sql
-- Time-based queries
CREATE INDEX idx_custom_jwt_metadata_issued ON custom.jwt_metadata (issued_at);
CREATE INDEX idx_tara_jwt_metadata_issued ON tara.jwt_metadata (issued_at);

-- Claim-based searches
CREATE INDEX idx_custom_jwt_metadata_claims ON custom.jwt_metadata USING gin (to_tsvector('english', claim_keys));

-- OAuth state cleanup
CREATE INDEX idx_tara_oauth_state_created ON tara.oauth_state (created_at);
```

### Query Patterns

**Common Operations:**
```sql
-- Token validation (high frequency)
SELECT jwt_uuid FROM custom.denylist WHERE jwt_uuid = ?;

-- Cleanup operations (scheduled)
DELETE FROM custom.jwt_metadata WHERE expires_at < ?;

-- Analytics queries (low frequency)
SELECT claim_keys, COUNT(*) FROM custom.jwt_metadata GROUP BY claim_keys;
```

## Security Architecture

### Schema-Level Security

**Access Control:**
```sql
-- Application user permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA custom TO tim_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tara TO tim_app;

-- Read-only analytics user
GRANT SELECT ON ALL TABLES IN SCHEMA custom TO tim_analytics;
GRANT SELECT ON ALL TABLES IN SCHEMA tara TO tim_analytics;

-- Backup user
GRANT SELECT ON ALL TABLES IN SCHEMA custom TO tim_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA tara TO tim_backup;
```

### Data Isolation Benefits

**1. Principle of Least Privilege:**
- Custom token operations cannot access TARA data
- TARA integration cannot modify custom tokens
- Clear separation of responsibilities

**2. Audit Trail Separation:**
- Custom token audit logs separate from TARA
- Compliance reporting focused on specific token types
- Security incident isolation

**3. Attack Surface Reduction:**
- Compromise of custom tokens doesn't affect TARA
- TARA vulnerabilities don't impact custom functionality
- Independent security patching

## Monitoring and Observability

### Schema-Specific Metrics

**Custom Schema Monitoring:**
```sql
-- Token generation rate
SELECT DATE(issued_at), COUNT(*)
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(issued_at);

-- Revocation rate
SELECT DATE(denylisted_at), COUNT(*)
FROM custom.denylist
WHERE denylisted_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(denylisted_at);
```

**TARA Schema Monitoring:**
```sql
-- OAuth flow success rate
SELECT DATE(created_at), COUNT(*)
FROM tara.oauth_state
WHERE created_at > NOW() - INTERVAL '1 day'
GROUP BY DATE(created_at);

-- TARA token issuance
SELECT DATE(issued_at), COUNT(*)
FROM tara.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(issued_at);
```

### Health Checks

**Schema Integrity:**
```sql
-- Verify both schemas exist
SELECT schema_name FROM information_schema.schemata
WHERE schema_name IN ('custom', 'tara');

-- Table count verification
SELECT schemaname, COUNT(*) as table_count
FROM pg_tables
WHERE schemaname IN ('custom', 'tara')
GROUP BY schemaname;

-- Orphaned data detection
SELECT COUNT(*) as orphaned_denylist
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL;
```

## Best Practices

### Schema Design Guidelines

**1. Maintain Symmetry:**
- Keep table structures identical where possible
- Use consistent naming conventions
- Apply same indexing strategies

**2. Preserve Isolation:**
- No cross-schema foreign keys
- No shared tables or views
- Independent cleanup procedures

**3. Document Differences:**
- Clearly explain schema-specific features
- Document business rationale for separation
- Maintain architectural decision records

### Development Workflow

**1. Schema Changes:**
- Update both schemas simultaneously when applicable
- Test isolation thoroughly
- Document schema-specific variations

**2. Testing Strategy:**
- Test both schemas independently
- Verify no cross-schema dependencies
- Validate security isolation

**3. Deployment Process:**
- Deploy schema changes atomically
- Verify both schemas after deployment
- Monitor schema-specific metrics

---

## Related Documentation

- **[Database Setup](./01-database-setup.md)**: Initial database configuration
- **[Custom JWT Tables](./03-custom-tables.md)**: Detailed custom schema design
- **[TARA Tables](./04-tara-tables.md)**: TARA-specific table design
- **[JWT-Database Correlation](./05-jwt-correlation.md)**: Token-to-database linking