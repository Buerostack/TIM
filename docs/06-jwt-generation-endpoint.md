# JWT Generation Endpoint

## Overview

The `/jwt/custom/generate` endpoint allows programmatic generation of signed JWT tokens with custom claims. This endpoint provides a secure way to create tokens for authentication and authorization without requiring user interaction through TARA.

**📋 For audience configuration details, see: [JWT Audience Configuration](./08-jwt-audience-configuration.md)**

## API Reference

**URL**: `POST /jwt/custom/generate`
**Content-Type**: `application/json`
**Authentication**: None required

### Request Format

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "API_TOKEN",
    "content": {
      "sub": "user123",
      "role": "admin",
      "department": "engineering",
      "permissions": ["read", "write", "delete"]
    },
    "audience": "payment-service",    // See audience docs for details
    "expirationInMinutes": 60,
    "setCookie": false
  }'
```

### Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `JWTName` | string | No | `"JWTTOKEN"` | Logical name for the token, used as cookie name if `setCookie=true` |
| `content` | object | No | `{}` | Custom claims to include in the JWT payload |
| `audience` | string/array | No | See [audience docs](./08-jwt-audience-configuration.md) | Target audience(s) for the token |
| `expirationInMinutes` | number | No | `60` | Token expiration time in minutes |
| `setCookie` | boolean | No | `false` | Whether to set the JWT as an HTTP-only cookie |

### Response Format

```json
{
  "status": "created",
  "name": "API_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ...",
  "expiresAt": "2025-09-28T18:18:18Z"
}
```

### Response Headers

When `setCookie: true`:
```
Set-Cookie: API_TOKEN=eyJ...; Path=/; HttpOnly
```

## Data Flow

### Request Processing Flow

```
Browser/Client → TIM Controller → Service Layer → Database
     ↓               ↓              ↓             ↓
   Request       Parameter      JWT Generation  Metadata
   Parsing       Validation     + Signing       Persistence
     ↓               ↓              ↓             ↓
   Response ← Response Builder ← Token Creation ← DB Commit
```

### Detailed Steps

1. **Request Reception** (`CustomJwtController.java:25`)
   - HTTP POST received at `/jwt/custom/generate`
   - JSON request body parsed into `CustomJwtGenerateRequest`

2. **Audience Processing** (`CustomJwtController.java:27-52`)
   - Determine final audience(s) based on configuration and request
   - Validate against allowed audiences if validation enabled
   - Apply defaults or return error if invalid

3. **JWT Generation** (`CustomJwtService.java:14`)
   - Call service with parameters including audience list
   - Service uses `JwtSignerService` for actual signing

4. **Token Signing** (`JwtSignerService.java:18`)
   - Create JWT claims with issuer, audience(s), expiration
   - Add custom claims from request content
   - Sign with RSA private key from keystore
   - Generate unique JWT ID (jti)

5. **Database Persistence** (`CustomJwtService.java:16`)
   - Parse signed JWT to extract metadata
   - Create `CustomJwtMetadata` entity
   - Save to `custom.jwt_metadata` table

6. **Response Construction** (`CustomJwtController.java:62-70`)
   - Create `TokenResponse` object
   - Set cookie header if requested
   - Return JSON response with token details

## Source Code Components

### Core Files

| File | Purpose | Key Methods |
|------|---------|-------------|
| `CustomJwtController.java:25-71` | HTTP endpoint handler | `generate(@RequestBody CustomJwtGenerateRequest)` |
| `CustomJwtService.java:14-18` | Business logic service | `generate(String, Map, String, List<String>, long)` |
| `JwtSignerService.java:18-21` | JWT signing utilities | `sign(Map, String, List<String>, long)` |
| `CustomJwtGenerateRequest.java` | Request DTO | Audience handling, parameter binding |
| `JwtAudienceConfig.java` | Audience configuration | Validation, allowed audiences |

### Configuration

| Property | Purpose |
|----------|---------|
| `jwt.signature.key-store` | RSA keystore location |
| `jwt.custom.audience.*` | Audience validation settings |

## JWT Token Structure

### Header
```json
{
  "kid": "jwtsign",
  "alg": "RS256"
}
```

### Payload (Claims)
```json
{
  "aud": ["payment-service"],     // Audience(s) - configurable
  "sub": "user123",               // Subject from content
  "role": "admin",                // Custom claim from content
  "department": "engineering",    // Custom claim from content
  "iss": "TIM",                   // Issuer - always TIM
  "exp": 1759083497,              // Expiration timestamp
  "iat": 1759079897,              // Issued at timestamp
  "jti": "ed68de1a-3a43..."       // Unique JWT ID
}
```

## Testing

### Basic Tests

```bash
# Test 1: Simple generation
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "TEST", "content": {"sub": "user123"}}'

# Test 2: With custom audience
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "CUSTOM", "content": {"sub": "user456"}, "audience": "my-service"}'

# Test 3: Multiple audiences
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "MULTI", "content": {"sub": "user789"}, "audience": ["service1", "service2"]}'

# Test 4: Cookie generation
curl -v -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "COOKIE", "content": {"sub": "user"}, "setCookie": true}'
```

### Token Verification

```bash
# Generate token
RESPONSE=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "VERIFY", "content": {"sub": "testuser"}}')

TOKEN=$(echo "$RESPONSE" | jq -r '.token')

# Decode header and payload
echo "$TOKEN" | cut -d'.' -f1 | base64 -d | jq .  # Header
echo "$TOKEN" | cut -d'.' -f2 | base64 -d | jq .  # Payload

# Get public key for verification
curl http://localhost:8085/jwt/keys/public
```

### Database Verification

```sql
-- Check metadata persistence
SELECT jwt_uuid, claim_keys, issued_at, expires_at
FROM custom.jwt_metadata
ORDER BY issued_at DESC LIMIT 5;
```

## Error Handling

### Audience Validation Errors

When audience validation is enabled and invalid audience requested:

```json
{
  "error": "invalid_audience",
  "message": "One or more requested audiences are not allowed",
  "allowed_audiences": ["payment-service", "user-service"]
}
```

### Common Issues

1. **Build Cache Issues**: Use `docker-compose build --no-cache`
2. **Keystore Missing**: Check `/opt/tim/jwtkeystore.jks` exists
3. **Database Connection**: Verify PostgreSQL container is running
4. **Invalid Configuration**: Check application.properties for audience settings

## Security Considerations

- **Signing**: All tokens signed with RSA-256 private key
- **Expiration**: Configurable expiration times
- **Unique ID**: Each token has unique `jti` for tracking
- **Audience Scoping**: Prevents token misuse across services
- **Cookie Security**: HttpOnly cookies prevent XSS access

## Related Documentation

- **[JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**: Token validation API
- **[JWT Audience Configuration](./08-jwt-audience-configuration.md)**: Detailed audience setup and security