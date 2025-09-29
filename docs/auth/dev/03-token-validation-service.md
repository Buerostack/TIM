# User Story: OAuth2/OIDC Token Validation Service

## User Story
**As a** client application
**I want** to validate OAuth2/OIDC tokens received from users
**So that** I can verify user identity and authorize API access

## Background
After users authenticate via OAuth2 flow, client applications receive tokens that need validation. TIM should provide a token validation service that verifies tokens from any configured OAuth2/OIDC provider.

## Acceptance Criteria

### AC1: Token Validation Endpoint
**Given** a client application has received a token from a user
**When** making a POST request to `/oauth2/token/validate`
**Then** TIM should accept the token and optional provider hint
**And** determine which provider issued the token
**And** validate the token using appropriate validation rules
**And** return validation result with user information

### AC2: JWT Token Validation
**Given** an ID token or JWT access token needs validation
**When** validating the JWT token
**Then** TIM should:
- Parse the JWT header to determine key ID (kid)
- Fetch the appropriate public key from provider's JWKS endpoint
- Verify the token signature using the public key
- Validate token expiration (`exp` claim)
- Validate token not used before time (`nbf` claim, if present)
- Validate issuer (`iss` claim) matches expected provider
- Validate audience (`aud` claim) contains TIM's client ID
- Extract and return user claims

### AC3: Opaque Token Validation
**Given** an opaque (non-JWT) access token needs validation
**When** validating the opaque token
**Then** TIM should call the provider's token introspection endpoint
**And** verify the token is active and valid
**And** extract token metadata (scopes, expiration, user info)
**And** return standardized validation result

### AC4: Provider Detection
**Given** a token without explicit provider information
**When** determining which provider issued the token
**Then** TIM should:
- For JWT tokens: extract issuer from token claims
- Match issuer against configured provider configurations
- For opaque tokens: try introspection with each configured provider
- Cache successful provider matches to optimize subsequent requests

### AC5: Token Caching and Performance
**Given** high-volume token validation requests
**When** validating frequently used tokens
**Then** TIM should cache validation results with appropriate TTL
**And** respect token expiration times in cache TTL
**And** invalidate cache entries when tokens expire
**And** provide fast response times (<100ms for cached tokens)

### AC6: TARA-Specific Token Handling
**Given** a TARA-issued token needs validation
**When** processing the token
**Then** TIM should handle TARA's short token validity (40 seconds)
**And** extract TARA-specific claims:
- `personalcode` (Estonian personal identification code)
- `given_name` and `family_name`
- `amr` (authentication method used)
- `acr` (level of assurance)
**And** map these to standardized user profile format

### AC7: Error Handling and Response Format
**Given** various token validation scenarios
**When** validation succeeds or fails
**Then** TIM should return consistent response format:
```json
{
  "valid": true/false,
  "active": true/false,
  "provider": "provider_id",
  "expires_at": "2025-09-29T16:30:00Z",
  "user": {
    "sub": "user_identifier",
    "name": "User Name",
    "email": "user@example.com",
    "custom_claims": {}
  },
  "scopes": ["openid", "profile"],
  "error": "error_description" // only if validation failed
}
```

## Technical Requirements

### Validation Request Format
```json
{
  "token": "JWT_OR_OPAQUE_TOKEN",
  "provider": "optional_provider_hint",
  "token_type": "id_token|access_token|auto_detect"
}
```

### JWKS Key Management
- Fetch and cache provider public keys from JWKS endpoints
- Support key rotation by checking `kid` claim
- Refresh JWKS when unknown key IDs are encountered
- Cache keys with appropriate TTL (24 hours default)

### Token Introspection Support
```http
POST /introspect HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Authorization: Basic CLIENT_CREDENTIALS

token=ACCESS_TOKEN&
token_type_hint=access_token
```

## Security Requirements
- Validate all JWT signatures before trusting claims
- Implement proper key management and rotation
- Rate limit validation requests to prevent abuse
- Log validation failures for security monitoring
- Sanitize and validate all input parameters
- Use secure HTTP clients with proper SSL/TLS validation

## Performance Requirements
- Cache validation results for identical tokens
- Optimize JWKS fetching and caching
- Support concurrent validation requests
- Response time: <50ms for cached results, <200ms for new validations
- Support high-throughput scenarios (1000+ req/sec)

## Provider-Specific Considerations

### TARA Estonia
- Handle 40-second token validity
- Support Estonian personal code validation
- Map authentication methods correctly
- Handle cross-border eIDAS claims

### Google OAuth2
- Support Google's token format
- Handle Google-specific scopes
- Map Google user profile correctly

### Azure AD
- Support Microsoft's token format
- Handle Azure AD specific claims
- Support both v1.0 and v2.0 endpoints

## Implementation Notes
- Use robust JWT library with security validations
- Implement circuit breaker pattern for provider calls
- Support token type auto-detection based on format
- Consider using async validation for better performance
- Implement proper logging for audit trails

## Definition of Done
- [ ] Token validation endpoint implemented
- [ ] JWT validation with signature verification
- [ ] Opaque token introspection support
- [ ] Provider detection and routing
- [ ] JWKS key management and caching
- [ ] Token result caching for performance
- [ ] TARA-specific token handling
- [ ] Standardized response format
- [ ] Comprehensive error handling
- [ ] Rate limiting and security measures
- [ ] Unit tests for all validation scenarios
- [ ] Load testing for performance validation
- [ ] Integration tests with real providers
- [ ] Security audit and penetration testing