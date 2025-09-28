# Fix Custom JWT Generation Endpoint

## User Story
**AS A** service or developer
**I WANT TO** generate custom JWT tokens via the `/jwt/custom/generate` endpoint
**SO THAT** I can receive properly signed JWT tokens with my specified claims and use them for authentication

## Current Issue
The endpoint currently returns only `{"status": "ok"}` instead of the complete token response with the actual JWT token, despite having the correct code structure in place.

## Root Cause Analysis
Based on code examination, the issue likely stems from:
1. Missing JWT signature configuration properties (jwt.signature.*)
2. JwtEncoder bean not being properly instantiated due to missing keystore
3. Service layer returning fallback response when JWT generation fails
4. No database entities/repositories configured for metadata persistence

## Acceptance Criteria

### AC1: JWT Configuration Setup
- [ ] Add missing JWT signature properties to application.properties
- [ ] Configure keystore path, password, type, and alias
- [ ] Ensure keystore file exists or auto-generation works
- [ ] Validate JwtEncoder bean is properly created and injected
- [ ] Test keystore loading and RSA key extraction

### AC2: JWT Token Generation
- [ ] Verify JwtEncoder successfully creates signed JWT tokens
- [ ] Include all required standard claims (iss, sub, aud, exp, iat, jti)
- [ ] Add custom claims from request content map
- [ ] Set proper token name claim as specified in request
- [ ] Generate unique JWT ID for each token

### AC3: Complete Token Response
- [ ] Return full TokenResponse with all fields populated:
  - status: "ok"
  - name: request.JWTName
  - token: actual signed JWT string
  - expiresAt: expiration timestamp
- [ ] Ensure response matches documented API format
- [ ] Handle JSON serialization of Instant fields properly

### AC4: Database Integration (Phase 1)
- [ ] Create CustomJwtMetadata JPA entity
- [ ] Create CustomJwtMetadataRepository interface
- [ ] Save token metadata to custom.jwt_metadata table on generation
- [ ] Include issued_at, expires_at, jwt_name, subject, claims_json
- [ ] Generate and store token hash for future reference

### AC5: Error Handling
- [ ] Handle keystore loading failures gracefully
- [ ] Return appropriate error response if JWT signing fails
- [ ] Validate request parameters (expirationInMinutes > 0, etc.)
- [ ] Handle database connection errors during metadata persistence
- [ ] Log errors without exposing sensitive information

### AC6: Testing and Validation
- [ ] Test endpoint with valid request returns complete JWT response
- [ ] Verify generated JWT can be decoded and signature verified
- [ ] Confirm token contains all specified claims
- [ ] Test database metadata is saved correctly
- [ ] Validate token works with public key verification endpoint

### AC7: Cookie Support (if requested)
- [ ] Implement setCookie functionality when setCookie=true
- [ ] Set HTTP-only secure cookie with JWT token
- [ ] Use configurable cookie name and domain
- [ ] Set appropriate cookie expiration matching JWT expiry

## Dependencies
- Keystore file must exist or auto-generation must work
- Database connection must be functional
- JPA entities and repositories must be implemented
- Application properties must include JWT configuration

## Testing Scenarios
```bash
# Valid request should return complete response
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "TEST_TOKEN",
    "content": {"sub": "user123", "role": "admin"},
    "expirationInMinutes": 60,
    "setCookie": false
  }'

# Expected response:
{
  "status": "ok",
  "name": "TEST_TOKEN",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...",
  "expiresAt": "2025-09-28T19:45:00Z"
}
```