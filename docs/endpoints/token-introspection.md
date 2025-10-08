# Token Introspection Endpoint

## Overview

The token introspection endpoint implements RFC 7662 (OAuth 2.0 Token Introspection) for validating and retrieving metadata about tokens issued by TIM.

## Endpoints

### POST /introspect

RFC 7662 compliant token introspection endpoint.

**Content-Type:** `application/x-www-form-urlencoded`

**Parameters:**
- `token` (required): The token to introspect
- `token_type_hint` (optional): Hint about the token type

**Response:** JSON object with token metadata

### POST /introspect (JSON)

Alternative JSON endpoint for easier client integration (not part of RFC 7662 but commonly supported).

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type_hint": "custom_jwt"
}
```

### GET /introspect/types

Returns information about supported token types.

**Response:**
```json
{
  "custom_jwt": "CustomJwtTokenValidator",
  "oauth2_access": "OAuth2TokenValidator"
}
```

## Response Format

Active token response:
```json
{
  "active": true,
  "sub": "user123",
  "iss": "TIM",
  "aud": "tim-audience",
  "exp": 1640995200,
  "iat": 1640908800,
  "jti": "token-id-123",
  "token_type": "custom_jwt",
  "role": "admin"
}
```

Inactive token response:
```json
{
  "active": false
}
```

## Token Type Detection

The introspection service automatically detects token types using:

1. **Explicit token_type claim**: Tokens issued by TIM include a `token_type` claim
2. **Heuristic detection**: For tokens without explicit type, uses issuer and audience claims
   - Custom JWTs: `iss=TIM` and `aud=tim-audience`
   - OAuth2 tokens: External issuers (non-TIM)

## Validation Process

For each token type, the introspection service:

1. **Signature verification**: Validates JWT signature
2. **Expiration check**: Ensures token is not expired
3. **Revocation check**: Checks against deny list (for custom JWTs)
4. **Claims extraction**: Returns standard and custom claims

## Security Considerations

- The endpoint is publicly accessible (no authentication required per RFC 7662)
- Failed validations return `{"active": false}` without detailed error information
- Revoked tokens are immediately marked as inactive
- All validation errors are logged for security monitoring

## Integration Examples

### Form-encoded request (RFC 7662)
```bash
curl -X POST http://localhost:8080/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### JSON request
```bash
curl -X POST http://localhost:8080/introspect \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."}'
```

### Check supported types
```bash
curl http://localhost:8080/introspect/types
```