# User Story: OAuth2 Authorization Code Flow

## User Story
**As a** client application user
**I want** to authenticate using OAuth2/OIDC providers (like TARA)
**So that** I can securely access TIM services using my existing identity

## Background
The Authorization Code Flow is the most secure OAuth2 flow for server-side applications. It involves redirecting users to the identity provider, obtaining an authorization code, and exchanging it for tokens.

## Acceptance Criteria

### AC1: Initiate Authorization
**Given** a client application wants to authenticate a user
**When** making a GET request to `/oauth2/auth/{provider}`
**Then** TIM should generate a secure state parameter
**And** optionally generate PKCE code_challenge (if supported)
**And** redirect user to provider's authorization endpoint with:
- `response_type=code`
- `client_id` (from provider config)
- `redirect_uri` (TIM's callback URL)
- `scope` (configured scopes for provider)
- `state` (generated secure value)
- `nonce` (for OIDC ID token validation)
- `code_challenge` & `code_challenge_method` (if PKCE enabled)

### AC2: Handle Authorization Callback
**Given** a user completes authentication at the provider
**When** the provider redirects to TIM's callback URL `/oauth2/callback/{provider}`
**Then** TIM should validate the state parameter matches stored value
**And** extract the authorization code from query parameters
**And** handle any error parameters (`error`, `error_description`)

### AC3: Exchange Code for Tokens
**Given** a valid authorization code is received
**When** exchanging the code for tokens
**Then** TIM should make POST request to provider's token endpoint with:
- `grant_type=authorization_code`
- `client_id` and `client_secret` (or client authentication)
- `redirect_uri` (must match authorization request)
- `code` (the received authorization code)
- `code_verifier` (if PKCE was used)
**And** receive access_token, id_token, and optional refresh_token
**And** validate all received tokens

### AC4: Token Validation
**Given** tokens are received from the provider
**When** validating the tokens
**Then** for ID tokens, TIM should:
- Verify JWT signature using provider's public keys
- Validate issuer matches expected provider
- Validate audience contains TIM's client_id
- Validate expiration time is in the future
- Validate nonce matches the sent value
- Extract user identity claims
**And** for access tokens, validate format and expiration

### AC5: Provider-Specific Scope Handling
**Given** different providers support different scopes
**When** initiating authorization
**Then** TIM should use provider-specific scope configuration
**And** handle TARA-specific scopes (`idcard`, `mid`, `smartid`, `eidas`)
**And** handle standard scopes (`openid`, `profile`, `email`)
**And** allow scope customization per authorization request

### AC6: Error Handling
**Given** various error conditions can occur
**When** errors happen during the flow
**Then** TIM should handle:
- User denying authorization (`error=access_denied`)
- Invalid authorization codes
- Token endpoint failures
- Invalid tokens or signatures
- Network timeouts and connectivity issues
**And** provide appropriate error responses to client applications
**And** log errors for debugging

## Technical Requirements

### Authorization URL Structure
```
https://provider.example.com/auth?
  response_type=code&
  client_id=CLIENT_ID&
  redirect_uri=https://tim.example.com/oauth2/callback/provider&
  scope=openid+profile+email&
  state=SECURE_RANDOM_STATE&
  nonce=SECURE_RANDOM_NONCE
```

### Token Exchange Request
```http
POST /token HTTP/1.1
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
client_id=CLIENT_ID&
client_secret=CLIENT_SECRET&
redirect_uri=https://tim.example.com/oauth2/callback/provider&
code=AUTHORIZATION_CODE
```

### Token Response
```json
{
  "access_token": "ACCESS_TOKEN",
  "token_type": "Bearer",
  "expires_in": 3600,
  "id_token": "ID_TOKEN_JWT",
  "refresh_token": "REFRESH_TOKEN"
}
```

## Security Requirements
- Use cryptographically secure random generators for state and nonce
- Implement PKCE for additional security (RFC 7636)
- Validate all redirect URIs against whitelist
- Implement proper CSRF protection with state parameter
- Use HTTPS for all communications
- Store sensitive data (codes, tokens) securely and temporarily

## TARA-Specific Requirements
- Support TARA's authentication method scopes
- Handle TARA's short token validity (40 seconds)
- Extract TARA-specific claims (personalcode, authentication method)
- Support cross-border eIDAS authentication

## Implementation Notes
- Store state/nonce in secure server-side cache with short TTL
- Implement proper session management
- Consider rate limiting on authorization endpoints
- Support both query and fragment response modes
- Handle provider-specific variations gracefully

## Definition of Done
- [ ] Authorization initiation endpoint working
- [ ] Secure state and nonce generation
- [ ] Authorization callback handling
- [ ] Token exchange implementation
- [ ] ID token validation with JWT signature verification
- [ ] Provider-specific scope configuration
- [ ] Comprehensive error handling
- [ ] PKCE support for enhanced security
- [ ] Unit tests for all flow steps
- [ ] Integration tests with TARA and test providers
- [ ] Security review and penetration testing