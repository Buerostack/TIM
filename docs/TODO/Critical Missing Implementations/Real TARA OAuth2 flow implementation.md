# Real TARA OAuth2 Flow Implementation

## User Story
**AS A** citizen of Estonia
**I WANT TO** authenticate using my national ID through TARA
**SO THAT** I can securely access services that require government-verified identity

## Acceptance Criteria

### AC1: TARA Login Initiation
- [ ] `GET /tara/login` redirects to actual TARA authorization endpoint
- [ ] OAuth2 state parameter is generated and stored in database
- [ ] PKCE code verifier is generated and stored
- [ ] Redirect URI is properly configured
- [ ] All required TARA parameters are included (scope, client_id, etc.)

### AC2: TARA Callback Handling
- [ ] `GET /tara/callback` validates the state parameter
- [ ] Authorization code is exchanged for access token with TARA
- [ ] PKCE code verifier is used in token exchange
- [ ] User identity information is retrieved from TARA
- [ ] JWT is generated with TARA user claims
- [ ] JWT is returned as configured (cookie/header)

### AC3: Error Handling
- [ ] Invalid state parameter returns appropriate error
- [ ] TARA authorization errors are handled gracefully
- [ ] Network errors during TARA communication are handled
- [ ] Expired OAuth states are cleaned up
- [ ] User-friendly error messages are displayed

### AC4: Security
- [ ] State parameter prevents CSRF attacks
- [ ] PKCE prevents authorization code interception
- [ ] Sensitive data is not logged
- [ ] OAuth state has reasonable expiration time