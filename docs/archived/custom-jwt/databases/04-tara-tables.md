# TARA Tables

## Overview

The `tara` schema contains tables specifically designed for TARA (Estonian e-ID) OAuth integration. While structurally similar to custom tables, these serve the distinct purpose of managing government identity tokens and OAuth authentication flows.

## Why TARA Needs Separate Tables

### TARA (Tell Authentication and Registration Authority)

**What is TARA?**
- **Estonian Government Service**: Official e-ID authentication system
- **Citizen Authentication**: Enables Estonian ID card, Mobile-ID, and e-Residency access
- **Government Compliance**: Meets Estonian e-governance security standards
- **OAuth 2.0 Provider**: Standard OAuth flows with government-grade security

### Separation Rationale

**1. Compliance Requirements:**
- **Government Standards**: Different security and audit requirements
- **Data Protection**: GDPR compliance for citizen identity data
- **Retention Policies**: Government-mandated token retention periods
- **Audit Trails**: Separate logging for government authentication

**2. Operational Independence:**
- **Service Isolation**: TARA issues can't affect custom token functionality
- **Performance**: Different query patterns and load characteristics
- **Maintenance**: Independent cleanup and optimization schedules
- **Security**: Breach isolation between government and custom systems

**3. Legal and Regulatory:**
- **Jurisdiction**: Estonian government regulations
- **Certification**: Meeting public sector certification requirements
- **Privacy**: Different privacy protection requirements for citizen data
- **Access Control**: Government-specific access patterns

## Table Structure

### `tara.jwt_metadata`

**Purpose**: Track JWT tokens issued through TARA OAuth authentication flows.

#### Schema Definition
```sql
CREATE TABLE tara.jwt_metadata (
    jwt_uuid   UUID PRIMARY KEY,                       -- JWT's 'jti' claim (unique identifier)
    claim_keys TEXT NOT NULL,                          -- Comma-separated claim names
    issued_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,   -- Token issuance timestamp
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL    -- Token expiration timestamp
);

-- Primary key index (automatic)
-- jwt_metadata_pkey PRIMARY KEY, btree (jwt_uuid)
```

#### Identical Structure, Different Purpose

**Why Identical to Custom Schema?**
1. **Consistent Operations**: Same JWT validation logic
2. **Code Reuse**: Shared service patterns for both token types
3. **Developer Experience**: Familiar patterns across both systems
4. **Maintenance**: Single codebase handles both schemas

**Key Differences in Usage:**
- **Claim Types**: Government identity claims (personal code, name, etc.)
- **Retention**: Longer retention for government audit requirements
- **Access Patterns**: Different query patterns for citizen authentication
- **Security**: Higher security requirements for government tokens

#### TARA-Specific Claim Examples
```sql
-- TARA tokens typically contain government identity claims
claim_keys examples:
- "sub,given_name,family_name,personal_code"    -- Estonian ID card
- "sub,mobile_number,personal_code"             -- Mobile-ID
- "sub,date_of_birth,personal_code,nationality" -- e-Residency
- "sub,auth_time,amr"                          -- Authentication method reference
```

### `tara.denylist`

**Purpose**: Manage revocation of TARA-issued JWT tokens.

#### Schema Definition
```sql
CREATE TABLE tara.denylist (
    jwt_uuid      UUID PRIMARY KEY,                     -- Links to tara.jwt_metadata.jwt_uuid
    denylisted_at TIMESTAMP NOT NULL DEFAULT now(),     -- Revocation timestamp
    expires_at    TIMESTAMP NOT NULL                    -- Original expiration (for cleanup)
);

-- Indexes
-- denylist_pkey PRIMARY KEY, btree (jwt_uuid)
-- idx_tara_denylist_exp btree (expires_at)  -- Cleanup optimization
```

#### TARA-Specific Revocation Scenarios

**Government-Specific Revocation Reasons:**
1. **ID Card Revocation**: Physical ID card is revoked or expires
2. **Mobile-ID Suspension**: Mobile authentication service suspended
3. **e-Residency Status Change**: e-Residency status revoked
4. **Security Incident**: Government security breach response
5. **Legal Requirements**: Court orders or regulatory requirements

**Usage Patterns:**
```java
// TARA-specific revocation handling
public void revokeTaraToken(String token, String reason) {
    var jwt = SignedJWT.parse(token);
    var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

    var dl = new TaraDenylist();
    dl.setJwtUuid(jti);
    dl.setDenylistedAt(Instant.now());
    dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
    // Additional TARA-specific logging for compliance
    auditLogger.logTaraRevocation(jti, reason);
    taraDenylistRepo.save(dl);
}
```

### `tara.allowlist`

**Purpose**: Optional explicit allowlist for TARA tokens (future functionality).

#### Schema Definition
```sql
CREATE TABLE tara.allowlist (
    jwt_hash   TEXT PRIMARY KEY,                        -- Hash/identifier of allowed token
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL     -- Expiration for cleanup
);

-- Indexes
-- allowlist_pkey PRIMARY KEY, btree (jwt_hash)
-- idx_tara_allowlist_exp btree (expires_at)  -- Cleanup optimization
```

#### Potential TARA Use Cases
- **Emergency Access**: Government emergency authentication scenarios
- **High-Security Services**: Additional validation for sensitive government services
- **Compliance Override**: Temporary access during system maintenance
- **Special Permissions**: Elevated access for government officials

### `tara.oauth_state`

**Purpose**: Manage OAuth PKCE (Proof Key for Code Exchange) state for secure TARA authentication flows.

#### Schema Definition
```sql
CREATE TABLE tara.oauth_state (
    state         TEXT PRIMARY KEY,                    -- OAuth state parameter
    created_at    TIMESTAMP NOT NULL DEFAULT now(),    -- Creation time
    pkce_verifier TEXT                                 -- PKCE code verifier
);

-- Primary key index (automatic)
-- oauth_state_pkey PRIMARY KEY, btree (state)
```

#### TARA OAuth Flow Integration

**PKCE Security Flow:**
1. **Authorization Request**: Generate state and PKCE challenge
2. **State Storage**: Store in `tara.oauth_state` table
3. **TARA Redirect**: User authenticates with Estonian e-ID
4. **Callback Processing**: Verify state and exchange code for token
5. **Cleanup**: Remove used state from table

**Field Details:**

**`state` (TEXT, Primary Key)**
- **Purpose**: OAuth state parameter for CSRF protection
- **Format**: Cryptographically secure random string
- **Uniqueness**: Each OAuth flow has unique state
- **Security**: Prevents cross-site request forgery attacks

**`created_at` (TIMESTAMP, NOT NULL, DEFAULT now())**
- **Purpose**: Track OAuth flow timing
- **Cleanup**: Remove expired states (typically 10-15 minutes)
- **Monitoring**: Detect stuck or abandoned OAuth flows

**`pkce_verifier` (TEXT, NULL)**
- **Purpose**: PKCE code verifier for enhanced security
- **Standard**: RFC 7636 - Proof Key for Code Exchange
- **Security**: Prevents authorization code interception attacks
- **Optional**: May be null for non-PKCE flows

#### TARA OAuth Implementation

**Current Status**: Basic OAuth endpoints exist but full TARA integration is not yet implemented.

**Existing Controller:**
```java
@RestController
@RequestMapping("/tara")
public class TaraController {
    @GetMapping("/login")
    public ResponseEntity<?> login() {
        // Redirects to TARA callback (placeholder implementation)
        return ResponseEntity.status(302)
            .location(URI.create("/tara/callback?code=dummy&state=dummy"))
            .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code, @RequestParam String state) {
        // Process TARA OAuth callback
        return ResponseEntity.ok(Map.of(
            "status", "callback-received",
            "cookieName", BuildConstants.TIM_TARA_JWT_NAME
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("status", "logged-out"));
    }
}
```

**Full Implementation Example:**
```java
// Complete TARA OAuth flow with oauth_state table
public class TaraOAuthService {

    public String initiateLogin(HttpServletRequest request) {
        // Generate OAuth state and PKCE parameters
        String state = generateSecureState();
        String pkceVerifier = generatePkceVerifier();
        String pkceChallenge = generatePkceChallenge(pkceVerifier);

        // Store state in database
        var oauthState = new TaraOauthState();
        oauthState.setState(state);
        oauthState.setPkceVerifier(pkceVerifier);
        oauthState.setCreatedAt(Instant.now());
        taraOauthStateRepo.save(oauthState);

        // Build TARA authorization URL
        return "https://tara.ria.ee/oidc/authorize?" +
               "response_type=code&" +
               "client_id=" + taraClientId + "&" +
               "redirect_uri=" + redirectUri + "&" +
               "scope=openid&" +
               "state=" + state + "&" +
               "code_challenge=" + pkceChallenge + "&" +
               "code_challenge_method=S256";
    }

    public String handleCallback(String code, String state) {
        // Verify and retrieve stored state
        var storedState = taraOauthStateRepo.findById(state)
            .orElseThrow(() -> new SecurityException("Invalid OAuth state"));

        // Exchange code for token using PKCE verifier
        String taraToken = exchangeCodeForToken(code, storedState.getPkceVerifier());

        // Parse and store TARA token metadata
        var jwt = SignedJWT.parse(taraToken);
        var jti = UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

        var meta = new TaraJwtMetadata();
        meta.setJwtUuid(jti);
        meta.setClaimKeys(String.join(",", jwt.getJWTClaimsSet().getClaims().keySet()));
        meta.setIssuedAt(jwt.getJWTClaimsSet().getIssueTime().toInstant());
        meta.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant());
        taraJwtMetadataRepo.save(meta);

        // Cleanup used OAuth state
        taraOauthStateRepo.delete(storedState);

        return taraToken;
    }
}
```

## Comparison with Custom Tables

### Structural Similarities

**Identical Table Patterns:**
```sql
-- Both schemas have identical structures
{schema}.jwt_metadata (jwt_uuid, claim_keys, issued_at, expires_at)
{schema}.denylist (jwt_uuid, denylisted_at, expires_at)
{schema}.allowlist (jwt_hash, expires_at)
```

**Shared Operations:**
- Token validation logic
- Revocation mechanisms
- Cleanup procedures
- Analytics queries

### Key Differences

| Aspect | Custom Schema | TARA Schema |
|--------|---------------|-------------|
| **Token Source** | TIM generates internally | TARA OAuth provider |
| **Claims** | Custom business logic | Government identity data |
| **Validation** | Internal signature verification | TARA public key validation |
| **Retention** | Business-driven cleanup | Government audit requirements |
| **Access Control** | Application-specific | Government compliance |
| **OAuth Support** | Not applicable | Full OAuth 2.0 flow |
| **PKCE Support** | Not needed | Required for security |

### Data Flow Differences

**Custom Token Flow:**
```
API Request → TIM generates JWT → Store in custom.jwt_metadata → Return token
```

**TARA Token Flow:**
```
User Login → OAuth redirect to TARA → User authenticates with e-ID →
TARA callback → Code exchange → JWT received → Store in tara.jwt_metadata
```

## Performance Considerations

### TARA-Specific Optimizations

**OAuth State Cleanup:**
```sql
-- Frequent cleanup of expired OAuth states (every 5 minutes)
DELETE FROM tara.oauth_state
WHERE created_at < NOW() - INTERVAL '15 minutes';
```

**TARA Query Patterns:**
```sql
-- TARA tokens may have different query patterns
-- More time-based queries for government audit
SELECT * FROM tara.jwt_metadata
WHERE issued_at BETWEEN ? AND ?
ORDER BY issued_at;

-- Personal code lookups (if storing government IDs)
SELECT * FROM tara.jwt_metadata
WHERE claim_keys LIKE '%personal_code%'
  AND issued_at > NOW() - INTERVAL '1 year';
```

### Index Recommendations

**TARA-Specific Indexes:**
```sql
-- OAuth state cleanup optimization
CREATE INDEX idx_tara_oauth_state_created ON tara.oauth_state (created_at);

-- Government audit queries
CREATE INDEX idx_tara_jwt_metadata_issued ON tara.jwt_metadata (issued_at);

-- Compliance reporting
CREATE INDEX idx_tara_denylist_denylisted ON tara.denylist (denylisted_at);
```

## Security and Compliance

### Government Security Requirements

**Enhanced Security Measures:**
1. **Longer Audit Retention**: Government tokens kept longer for compliance
2. **Enhanced Logging**: All TARA operations logged for audit
3. **Access Control**: Stricter access controls for government data
4. **Encryption**: Additional encryption for sensitive government claims

**Compliance Monitoring:**
```sql
-- Monthly compliance report
SELECT
    DATE_TRUNC('month', issued_at) as month,
    COUNT(*) as tokens_issued,
    COUNT(DISTINCT SUBSTRING(claim_keys FROM 'personal_code[^,]*')) as unique_citizens
FROM tara.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', issued_at)
ORDER BY month;

-- Revocation tracking for compliance
SELECT
    DATE(denylisted_at) as revocation_date,
    COUNT(*) as revocations,
    STRING_AGG(DISTINCT 'Security incident', ', ') as reasons
FROM tara.denylist
WHERE denylisted_at > NOW() - INTERVAL '90 days'
GROUP BY DATE(denylisted_at);
```

### Data Protection

**GDPR Compliance:**
- **Right to be Forgotten**: Ability to remove citizen data
- **Data Minimization**: Only store necessary token metadata
- **Purpose Limitation**: TARA data used only for authentication
- **Retention Limits**: Government-specified retention periods

**Privacy Protection:**
```sql
-- Anonymize expired TARA tokens (GDPR compliance)
UPDATE tara.jwt_metadata
SET claim_keys = 'anonymized'
WHERE expires_at < NOW() - INTERVAL '2 years'
  AND claim_keys LIKE '%personal_code%';
```

## Monitoring and Analytics

### TARA-Specific Metrics

**Government Authentication Analytics:**
```sql
-- Estonian e-ID usage patterns
SELECT
    CASE
        WHEN claim_keys LIKE '%mobile%' THEN 'Mobile-ID'
        WHEN claim_keys LIKE '%smart_id%' THEN 'Smart-ID'
        WHEN claim_keys LIKE '%id_card%' THEN 'ID Card'
        ELSE 'Other'
    END as auth_method,
    COUNT(*) as usage_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM tara.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '30 days'
GROUP BY auth_method;

-- Peak authentication times (government services)
SELECT
    EXTRACT(HOUR FROM issued_at) as hour_of_day,
    COUNT(*) as authentications,
    ROUND(AVG(COUNT(*)) OVER (), 2) as daily_average
FROM tara.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '7 days'
GROUP BY EXTRACT(HOUR FROM issued_at)
ORDER BY hour_of_day;
```

**OAuth Flow Monitoring:**
```sql
-- OAuth flow success rate
SELECT
    DATE(created_at) as date,
    COUNT(*) as flows_initiated,
    (SELECT COUNT(*) FROM tara.jwt_metadata WHERE DATE(issued_at) = DATE(o.created_at)) as flows_completed,
    ROUND(
        (SELECT COUNT(*) FROM tara.jwt_metadata WHERE DATE(issued_at) = DATE(o.created_at)) * 100.0 / COUNT(*),
        2
    ) as success_rate
FROM tara.oauth_state o
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date;

-- Abandoned OAuth flows
SELECT COUNT(*) as abandoned_flows
FROM tara.oauth_state
WHERE created_at < NOW() - INTERVAL '1 hour';
```

## Integration with Application Code

### TARA Entity Classes

**TaraJwtMetadata Entity:**
```java
@Entity
@Table(name = "jwt_metadata", schema = "tara")
public class TaraJwtMetadata {
    @Id
    @Column(name = "jwt_uuid")
    private UUID jwtUuid;

    @Column(name = "claim_keys")
    private String claimKeys;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Getters and setters...
}
```

**TaraOauthState Entity:**
```java
@Entity
@Table(name = "oauth_state", schema = "tara")
public class TaraOauthState {
    @Id
    private String state;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "pkce_verifier")
    private String pkceVerifier;

    // Getters and setters...
}
```

### Repository Interfaces

**TARA Repositories:**
```java
@Repository
public interface TaraJwtMetadataRepo extends JpaRepository<TaraJwtMetadata, UUID> {
    List<TaraJwtMetadata> findByIssuedAtBetween(Instant start, Instant end);

    @Query("SELECT COUNT(t) FROM TaraJwtMetadata t WHERE t.claimKeys LIKE %:method%")
    long countByAuthenticationMethod(@Param("method") String method);
}

@Repository
public interface TaraOauthStateRepo extends JpaRepository<TaraOauthState, String> {
    void deleteByCreatedAtBefore(Instant cutoff);

    @Query("SELECT COUNT(t) FROM TaraOauthState t WHERE t.createdAt > :since")
    long countActiveFlows(@Param("since") Instant since);
}
```

## Maintenance Procedures

### TARA-Specific Cleanup

**Frequent OAuth State Cleanup:**
```sql
-- Run every 5 minutes (OAuth states are short-lived)
DELETE FROM tara.oauth_state
WHERE created_at < NOW() - INTERVAL '15 minutes';
```

**Government Audit Retention:**
```sql
-- Keep TARA metadata longer for government compliance
-- Run monthly
DELETE FROM tara.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '2 years';  -- Longer retention

-- Clean TARA denylist normally
DELETE FROM tara.denylist
WHERE expires_at < NOW();
```

**Compliance Archival:**
```sql
-- Archive old TARA data for compliance before deletion
INSERT INTO tara.jwt_metadata_archive
SELECT * FROM tara.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '1 year';

-- Then delete from active table
DELETE FROM tara.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '1 year';
```

---

## Related Documentation

- **[Schema Architecture](./02-schema-architecture.md)**: Overall database design rationale
- **[Custom JWT Tables](./03-custom-tables.md)**: Comparison with custom implementation
- **[JWT-Database Correlation](./05-jwt-correlation.md)**: Token linking mechanisms
- **[Database Operations](./06-operations.md)**: Maintenance and monitoring procedures