# JWT Generation After TARA Authentication

## User Story
**AS A** service consuming TARA-authenticated tokens
**I WANT TO** receive a standardized JWT after successful TARA authentication
**SO THAT** I can validate user identity and access claims consistently

## Acceptance Criteria

### AC1: TARA Claims Mapping
- [ ] Extract user identity from TARA response (personal code, name, etc.)
- [ ] Map TARA claims to standard JWT claims (sub, given_name, family_name)
- [ ] Include TARA-specific claims (authentication method, level of assurance)
- [ ] Set appropriate issuer claim identifying TIM as token issuer

### AC2: JWT Structure
- [ ] Use same RSA signing key as custom JWTs
- [ ] Include standard claims: iss, sub, aud, exp, iat, jti
- [ ] Add custom claims from TARA user data
- [ ] Set expiration time according to session policy
- [ ] Generate unique JWT ID for tracking

### AC3: Token Metadata Persistence
- [ ] Store JWT metadata in tara.jwt_metadata table
- [ ] Record issuance time and expiration
- [ ] Store TARA session identifier if available
- [ ] Link to original OAuth state for audit trail

### AC4: Cookie/Header Management
- [ ] Set JWT as HTTP-only secure cookie if requested
- [ ] Use configured cookie name (TIM_TARA_JWT)
- [ ] Set appropriate cookie domain and path
- [ ] Handle SameSite and secure flags properly
- [ ] Support both cookie and header-based token delivery

### AC5: Error Scenarios
- [ ] Handle missing or invalid TARA user data
- [ ] Graceful degradation if JWT signing fails
- [ ] Proper error responses for invalid authentication
- [ ] Log security events without exposing sensitive data