# JWT Validation Endpoints

## User Story
**AS A** service that needs to verify JWT tokens
**I WANT TO** validate tokens issued by TIM through dedicated endpoints
**SO THAT** I can ensure token authenticity and extract user claims securely

## Acceptance Criteria

### AC1: Token Validation Endpoint
- [ ] `POST /jwt/validate` accepts JWT in request body
- [ ] Validates JWT signature using TIM's public key
- [ ] Checks token expiration and not-before claims
- [ ] Verifies issuer matches TIM identifier
- [ ] Returns validation result with claims if valid

### AC2: Token Introspection Endpoint
- [ ] `POST /jwt/introspect` provides detailed token information
- [ ] Returns active status, expiration, and scope information
- [ ] Includes user claims if token is valid and active
- [ ] Follows RFC 7662 OAuth2 Token Introspection standard
- [ ] Supports both custom and TARA-issued tokens

### AC3: Allowlist/Denylist Enforcement
- [ ] Check token hash against denylist before validation
- [ ] Verify subject against subject-based deny rules
- [ ] Apply allowlist rules if configured
- [ ] Return appropriate error codes for blocked tokens
- [ ] Log validation attempts for audit purposes

### AC4: Performance Optimization
- [ ] Cache public keys to avoid repeated keystore access
- [ ] Implement rate limiting for validation endpoints
- [ ] Return fast responses for obviously invalid tokens
- [ ] Support batch validation for multiple tokens

### AC5: Error Handling
- [ ] Return standardized error responses
- [ ] Distinguish between malformed, expired, and revoked tokens
- [ ] Provide helpful error messages without security information
- [ ] Handle edge cases like missing or corrupted tokens