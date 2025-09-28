# TIM JWT Extend Endpoint

## Overview

The JWT Extend endpoint (`/jwt/custom/extend`) allows you to create a new JWT token based on an existing valid token, effectively extending its lifetime while preserving all custom claims. This is commonly used for token renewal in applications that need to maintain user sessions without requiring re-authentication.

The extend operation validates the existing token, extracts its custom claims, generates a new token with a fresh expiration time, and revokes the old token to prevent replay attacks.

## Endpoint Reference

### POST `/jwt/custom/extend`

Extends an existing JWT token by creating a new token with the same claims but updated expiration time.

#### Request Format

```http
POST /jwt/custom/extend
Content-Type: application/json

{
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "expirationInMinutes": 120,
  "setCookie": false
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `token` | string | **Yes** | The existing JWT token to extend |
| `expirationInMinutes` | integer | No | New expiration time in minutes (default: 60) |
| `setCookie` | boolean | No | Whether to set the new token as an HTTP cookie (default: false) |

#### Response Format

**Success Response (200 OK):**
```json
{
  "status": "extended",
  "name": "EXTENDED_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "expiresAt": "2025-09-29T00:08:15Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "invalid_request",
  "message": "Token is required"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "extend_denied",
  "message": "Token expired - cannot extend"
}
```

## Token Extension Process

### 1. Token Validation

The endpoint performs comprehensive validation of the existing token:

- **Signature Verification**: Ensures the token was signed by TIM
- **Expiration Check**: Token must not be expired (cannot extend expired tokens)
- **Revocation Check**: Token must not be in the denylist
- **Format Validation**: Token must be a valid JWT structure

### 2. Claims Preservation

All custom claims from the original token are preserved in the new token:

- **Preserved Claims**: All user-defined claims (e.g., `sub`, `role`, custom attributes)
- **Regenerated Claims**: Standard JWT claims (`iss`, `aud`, `exp`, `iat`, `jti`)
- **New JWT ID**: Each extended token gets a unique `jti` (JWT ID)

### 3. Old Token Revocation

The original token is automatically revoked when extension succeeds:

- Added to the denylist to prevent reuse
- Maintains security by ensuring only one active token per extension
- Original token becomes invalid immediately after successful extension

## Usage Examples

### Basic Token Extension

**Request:**
```bash
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MTAwODg3LCJpYXQiOjE3NTkwOTcyODcsImp0aSI6IjRlZTllOTQ1LWI4NmMtNGRlOS04OTk1LWU1NjE2OGU3ZDdiYSJ9.hCEBjDuoFLqL84b7aaHNvmIhazt7_8YpcJt2YEwk9EBvNVyoTl4PH5B0fSJ0qh5P00IWlyTbXGJ9chSs6iSptVkoce2-C8yuM9rwEhzOHdw3hyCREGfJWomHkcdYh5UJ-0sflfIV4ZWbkGlqEziGxEPmT55DXzp-X0vnRjTWC1Ew4JAhq8TE1bjhRQZijHtYf4iJaR6PMUX7LPrudxagW-hCWhV_Zr0UgVaxqSXqaJXCtFwThaujyqtGAvMXu91e-82q8CgwFo3accCzDysQNBC6vjj6rhrShmIbfj_Wre15tuFkf9moC9xS4l-CSkAFMX6kjvTYGl61Cd_9G24hhA",
    "expirationInMinutes": 120
  }'
```

**Response:**
```json
{
  "status": "extended",
  "name": "EXTENDED_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MTA0NDk1LCJpYXQiOjE3NTkwOTcyOTUsImp0aSI6IjYzMWViM2Y3LTJkYTYtNDNmMy1hOTQyLWVmMTJlZTllZGI3OCJ9.p-YNMX64Y-l3YF_xY_RGDJwAi4yxRBamnzDLCHvM_O9lo2JlR3Hn3UuuH-OBjbICX7ks-TUWxhrq8PiPT3EKANeYE3eC6RySg3kpjbalAYA2-g4usUkSp618Tpph7P5IzBI-lWLTiXCHaZYZhcXFqGUZvCQIbsGjt7x4xo7qvBsRKhUf7qZBOrvFG-P4uCeimHSJPfu2GuXJjDHsWfmooizkijYteuwAA8XKHOS1k2Q6UzMQQtX4NgwhUaI6jVK30MZSSa8AY3OarNKAjU1ijEXF3z_1epMy9aJKzikZR6D1LsJlRrERg242q-uM8l0pMu-OMBz6UM2zZrCisPKjhQ",
  "expiresAt": "2025-09-29T00:08:15.930510807Z"
}
```

### Extension with Cookie

**Request:**
```bash
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJraWQiOiJqd3RzaWduIi...",
    "expirationInMinutes": 180,
    "setCookie": true
  }'
```

**Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: EXTENDED_TOKEN=eyJraWQiOiJqd3RzaWduIi...; Path=/; HttpOnly

{
  "status": "extended",
  "name": "EXTENDED_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "expiresAt": "2025-09-29T03:08:15Z"
}
```

## Error Scenarios

### Invalid Request Format

**Missing Token:**
```bash
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{"expirationInMinutes": 60}'
```

**Response (400 Bad Request):**
```json
{
  "error": "invalid_request",
  "message": "Token is required"
}
```

### Invalid Token

**Malformed Token:**
```bash
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid.token.here",
    "expirationInMinutes": 60
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "extend_failed",
  "message": "Failed to extend token: Invalid token format"
}
```

### Token Extension Denied

**Expired Token:**
```bash
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJraWQiOiJqd3RzaWduIi...",
    "expirationInMinutes": 60
  }'
```

**Response (401 Unauthorized):**
```json
{
  "error": "extend_denied",
  "message": "Token expired - cannot extend"
}
```

**Invalid Signature:**
```json
{
  "error": "extend_denied",
  "message": "Invalid signature - cannot extend"
}
```

**Revoked Token:**
```json
{
  "error": "extend_denied",
  "message": "Token revoked - cannot extend"
}
```

## HTTP Status Codes

| Status Code | Scenario | Description |
|-------------|----------|-------------|
| **200 OK** | Success | Token successfully extended |
| **400 Bad Request** | Invalid Request | Missing token, invalid JSON, or malformed token |
| **401 Unauthorized** | Extension Denied | Token expired, revoked, or has invalid signature |

## Security Considerations

### Automatic Token Revocation

- **Old Token Invalidation**: Original token is immediately added to denylist
- **Prevents Replay Attacks**: Cannot reuse the original token after extension
- **Single Active Token**: Only the new token remains valid

### Token Lifetime Management

- **Maximum Extension Time**: Consider implementing maximum extension limits
- **Audit Trail**: All extensions are logged with original and new JWT IDs
- **Token Correlation**: Database tracks relationships between extended tokens

### Best Practices

1. **Extension Windows**: Consider allowing extension only within certain time windows
2. **Rate Limiting**: Implement rate limits to prevent abuse
3. **Monitoring**: Monitor extension patterns for unusual activity
4. **Client Implementation**: Handle token updates seamlessly in client applications

## Integration Patterns

### Session Management

```javascript
class TokenManager {
  async extendToken(currentToken, expirationMinutes = 60) {
    try {
      const response = await fetch('/jwt/custom/extend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token: currentToken,
          expirationInMinutes: expirationMinutes
        })
      });

      if (response.ok) {
        const result = await response.json();
        this.updateStoredToken(result.token);
        return result.token;
      } else {
        throw new Error('Token extension failed');
      }
    } catch (error) {
      console.error('Extension error:', error);
      this.handleExtensionFailure();
      return null;
    }
  }

  updateStoredToken(newToken) {
    localStorage.setItem('authToken', newToken);
  }

  handleExtensionFailure() {
    // Redirect to login or refresh page
    window.location.href = '/login';
  }
}
```

### Automatic Extension

```javascript
class AutoTokenExtender {
  constructor(tokenManager, extensionThreshold = 5) {
    this.tokenManager = tokenManager;
    this.extensionThreshold = extensionThreshold; // minutes before expiry
    this.checkInterval = 60000; // check every minute
    this.startAutoExtension();
  }

  startAutoExtension() {
    setInterval(() => {
      this.checkAndExtendToken();
    }, this.checkInterval);
  }

  async checkAndExtendToken() {
    const token = this.tokenManager.getCurrentToken();
    if (!token) return;

    const payload = JSON.parse(atob(token.split('.')[1]));
    const expiryTime = payload.exp * 1000; // convert to milliseconds
    const currentTime = Date.now();
    const timeUntilExpiry = expiryTime - currentTime;
    const minutesUntilExpiry = timeUntilExpiry / (1000 * 60);

    if (minutesUntilExpiry <= this.extensionThreshold) {
      console.log('Token expiring soon, extending...');
      await this.tokenManager.extendToken(token, 60);
    }
  }
}
```

## Database Schema Impact

### Token Metadata

Extended tokens create new entries in the `custom_jwt_metadata` table:

```sql
-- New token metadata
INSERT INTO custom.custom_jwt_metadata (
  jwt_uuid, issued_at, expires_at, claim_keys
) VALUES (
  '631eb3f7-2da6-43f3-a942-ef12ee9edb78',
  '2025-09-28T21:37:15Z',
  '2025-09-29T00:08:15Z',
  'sub,role'
);
```

### Token Denylist

Original tokens are added to the denylist:

```sql
-- Original token revocation
INSERT INTO custom.custom_denylist (
  jwt_uuid, denylisted_at, expires_at
) VALUES (
  '4ee9e945-b86c-4de9-8995-e56168e7d7ba',
  '2025-09-28T21:37:15Z',
  '2025-09-28T22:37:09Z'
);
```

## Monitoring and Observability

### Key Metrics

- **Extension Success Rate**: Percentage of successful extensions
- **Extension Frequency**: How often tokens are extended
- **Average Token Lifetime**: Time between generation and final expiration
- **Extension Failure Reasons**: Categories of extension denials

### Logging Examples

**Successful Extension:**
```
INFO [2025-09-28T21:37:15Z] JWT extended - original_jti=4ee9e945-b86c-4de9-8995-e56168e7d7ba new_jti=631eb3f7-2da6-43f3-a942-ef12ee9edb78 duration=120min status=200
```

**Extension Denied:**
```
WARN [2025-09-28T21:37:15Z] JWT extension denied - jti=4ee9e945-b86c-4de9-8995-e56168e7d7ba reason="Token expired - cannot extend" status=401
```

**Extension Failed:**
```
ERROR [2025-09-28T21:37:15Z] JWT extension failed - reason="Invalid token format" status=400
```

## Testing

### Manual Testing with curl

```bash
# 1. Generate a test token
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "TEST_TOKEN",
    "content": {"sub": "user123", "role": "admin"},
    "expirationInMinutes": 60
  }'

# 2. Extract the token from response and extend it
curl -X POST http://localhost:8085/jwt/custom/extend \
  -H "Content-Type: application/json" \
  -d '{
    "token": "PASTE_TOKEN_HERE",
    "expirationInMinutes": 120
  }'

# 3. Verify the original token is now revoked
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "ORIGINAL_TOKEN_HERE"}'
```

### Automated Test Suite

```javascript
describe('JWT Extend Endpoint', () => {
  let validToken;

  beforeEach(async () => {
    // Generate fresh token for each test
    const response = await generateToken({
      JWTName: 'TEST_TOKEN',
      content: { sub: 'user123', role: 'admin' },
      expirationInMinutes: 60
    });
    validToken = response.token;
  });

  test('Successfully extends valid token', async () => {
    const response = await extendToken({
      token: validToken,
      expirationInMinutes: 120
    });

    expect(response.status).toBe(200);
    expect(response.data.status).toBe('extended');
    expect(response.data.token).toBeTruthy();
    expect(response.data.name).toBe('EXTENDED_TOKEN');
  });

  test('Rejects missing token', async () => {
    const response = await extendToken({
      expirationInMinutes: 60
    });

    expect(response.status).toBe(400);
    expect(response.data.error).toBe('invalid_request');
  });

  test('Rejects invalid token', async () => {
    const response = await extendToken({
      token: 'invalid.token.here',
      expirationInMinutes: 60
    });

    expect(response.status).toBe(400);
    expect(response.data.error).toBe('extend_failed');
  });

  test('Original token becomes invalid after extension', async () => {
    // Extend the token
    await extendToken({
      token: validToken,
      expirationInMinutes: 120
    });

    // Verify original token is now invalid
    const validation = await validateToken(validToken);
    expect(validation.status).toBe(401);
    expect(validation.data.reason).toBe('Token revoked');
  });
});
```

## Comparison with Industry Standards

### OAuth 2.0 Token Refresh

**TIM Extend vs OAuth 2.0 Refresh:**
- ✅ Similar functionality (extends token lifetime)
- ✅ Revokes old token to prevent replay
- ✅ Preserves original claims and scopes
- ⚠️ Different endpoint naming (`/extend` vs `/token`)
- ⚠️ Single token model (vs separate access/refresh tokens)

### JWT Best Practices

**Security Alignment:**
- ✅ Short-lived tokens with extension capability
- ✅ Automatic revocation of extended tokens
- ✅ Signature verification before extension
- ✅ Claims preservation and validation

---

## Related Documentation

- **[JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**: Creating new JWT tokens
- **[JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**: Validating JWT tokens
- **[JWT Revocation Endpoint](./10-jwt-revocation-endpoint.md)**: Revoking JWT tokens
- **[HTTP Response Codes](./09-http-response-codes.md)**: Status codes and error handling