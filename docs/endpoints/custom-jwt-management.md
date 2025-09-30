# Custom JWT Management Endpoints

## Overview
TIM provides comprehensive JWT token management including generation, listing, extension, revocation, and validation. All endpoints support Bearer token authentication.

## Base URL
- Local Development: `http://localhost:8085`
- Production: Configure via environment variables

## Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### POST /jwt/custom/generate
**Description**: Generate a new custom JWT token
**Authentication**: None required for generation
**Content-Type**: `application/json`
**Request Body**:
```json
{
  "JWTName": "my-api-token",
  "content": {
    "sub": "user123",
    "role": "admin",
    "permissions": ["read", "write"]
  },
  "expirationInMinutes": 1440,
  "setCookie": false
}
```
**Response**:
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires": "2024-01-16T10:30:00Z",
  "jwtId": "uuid-token-id"
}
```

### POST /jwt/custom/list/me
**Description**: List all JWT tokens owned by the authenticated user
**Authentication**: Required (Bearer token)
**Headers**:
- `Authorization`: Bearer token (required)
**Content-Type**: `application/json`
**Request Body** (optional):
```json
{
  "includeRevoked": false,
  "limit": 50,
  "offset": 0
}
```
**Response**:
```json
{
  "tokens": [
    {
      "jwtId": "uuid-token-id",
      "name": "my-api-token",
      "subject": "user123",
      "issuedAt": "2024-01-15T10:30:00Z",
      "expiresAt": "2024-01-16T10:30:00Z",
      "revoked": false,
      "lastUsed": "2024-01-15T15:45:00Z"
    }
  ],
  "total": 1,
  "hasMore": false
}
```

### POST /jwt/custom/extend
**Description**: Extend expiration time of an existing JWT token
**Authentication**: Required (Bearer token)
**Headers**:
- `Authorization`: Bearer token (required)
**Content-Type**: `application/json`
**Request Body**:
```json
{
  "jwtId": "uuid-token-id",
  "additionalMinutes": 720
}
```
**Response**:
```json
{
  "success": true,
  "newExpiration": "2024-01-17T10:30:00Z",
  "jwtId": "uuid-token-id"
}
```

### POST /jwt/custom/revoke
**Description**: Revoke (invalidate) a JWT token
**Authentication**: Required (Bearer token)
**Headers**:
- `Authorization`: Bearer token (required)
**Content-Type**: `application/json`
**Request Body**:
```json
{
  "jwtId": "uuid-token-id",
  "reason": "No longer needed"
}
```
**Response**:
```json
{
  "success": true,
  "revokedAt": "2024-01-15T16:00:00Z",
  "jwtId": "uuid-token-id"
}
```

### POST /jwt/custom/validate
**Description**: Validate a JWT token and return its claims
**Authentication**: None required
**Content-Type**: `application/json`
**Request Body**:
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
**Response**:
```json
{
  "valid": true,
  "claims": {
    "sub": "user123",
    "role": "admin",
    "permissions": ["read", "write"],
    "iss": "TIM",
    "aud": "tim-audience",
    "exp": 1705483800,
    "iat": 1705397400,
    "jti": "uuid-token-id"
  },
  "revoked": false,
  "expiresAt": "2024-01-16T10:30:00Z"
}
```

## JWT Token Structure
Generated tokens include standard JWT claims:
- `iss`: Issuer (always "TIM")
- `aud`: Audience ("tim-audience")
- `sub`: Subject (user identifier)
- `exp`: Expiration timestamp
- `iat`: Issued at timestamp
- `jti`: JWT ID (unique identifier)

Custom claims can be added via the `content` field during generation.

## Security Features
- RSA256 signature algorithm
- Token revocation support with audit trail
- Configurable expiration times
- Bearer token authentication for management operations
- Smart idempotency for revocation operations

## Error Handling
- **400 Bad Request**: Invalid request format or missing required fields
- **401 Unauthorized**: Missing or invalid Bearer token
- **403 Forbidden**: Token exists but user doesn't have permission
- **404 Not Found**: JWT ID not found
- **409 Conflict**: Token already revoked (for revocation requests)
- **500 Internal Server Error**: Token generation or validation failure

## Rate Limiting
- Token generation: Limited per user per hour
- List operations: Standard API rate limits apply
- Validation: Higher limits for performance

## Best Practices
1. Store tokens securely on client side
2. Use appropriate expiration times based on security requirements
3. Revoke tokens when no longer needed
4. Regular token rotation for long-lived applications
5. Monitor token usage via the list endpoint