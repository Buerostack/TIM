# JWT Revocation Endpoint

## Overview

The JWT revocation endpoint provides secure token invalidation functionality, allowing applications to immediately revoke JWT tokens before their natural expiration. This is essential for security incidents, user logouts, and access control management.

**📋 For validation endpoint details, see: [JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**
**📋 For generation endpoint details, see: [JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**

## API Reference

### JWT Revocation Endpoint
```
POST /jwt/custom/revoke
```

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ...",
  "reason": "user_logout"
}
```

**Parameters:**
- `token` (string, required): The JWT token to revoke
- `reason` (string, optional): Reason for revocation (stored for audit purposes)

### Response Format

**Content-Type:** `application/json`

**HTTP Status Codes:**
- **200 OK**: Token was successfully revoked (newly revoked)
- **409 Conflict**: Token was already revoked (idempotent operation)
- **400 Bad Request**: Invalid request or token format

#### Success Responses

**Newly Revoked Token (HTTP 200)**
```json
{
  "status": "revoked",
  "message": "Token has been successfully revoked"
}
```

**Already Revoked Token (HTTP 409)**
```json
{
  "status": "already_revoked",
  "message": "Token was already revoked"
}
```

#### Error Responses

**Missing Token (HTTP 400)**
```json
{
  "error": "invalid_request",
  "message": "Token is required"
}
```

**Revocation Failed (HTTP 400)**
```json
{
  "error": "revocation_failed",
  "message": "Failed to revoke token: Invalid token format"
}
```

## How Token Revocation Works

### Revocation Process

1. **Token Parsing**: Extract JWT ID (`jti`) from the provided token
2. **Database Storage**: Add token UUID to `custom.denylist` table with optional reason
3. **Immediate Effect**: Token becomes invalid for all future validation requests
4. **Audit Trail**: Revocation timestamp, expiration, and reason stored for audit compliance

### Database Impact

**Before Revocation:**
```sql
-- Token exists only in jwt_metadata
SELECT jwt_uuid FROM custom.jwt_metadata WHERE jwt_uuid = 'token-jti';
-- Returns: token-jti

SELECT jwt_uuid FROM custom.denylist WHERE jwt_uuid = 'token-jti';
-- Returns: (empty)
```

**After Revocation:**
```sql
-- Token added to denylist with reason
SELECT jwt_uuid, denylisted_at, reason FROM custom.denylist WHERE jwt_uuid = 'token-jti';
-- Returns: token-jti, 2025-09-28 21:45:30, user_logout

-- Validation now fails
SELECT 'REVOKED' as status WHERE EXISTS (
  SELECT 1 FROM custom.denylist WHERE jwt_uuid = 'token-jti'
);
-- Returns: REVOKED
```

### Revocation Flow Diagram

```
Client Request → Parse JWT → Extract jti → Add to Denylist → Return Success
                     ↓             ↓             ↓              ↓
                Parse Error   Invalid jti   Database Error   HTTP 200
                     ↓             ↓             ↓              ↓
                HTTP 400      HTTP 400      HTTP 400      {"status":"revoked"}
```

## Use Cases

### 1. User Logout
```javascript
// Revoke user's session token on logout
async function logout(sessionToken) {
  const response = await fetch('/jwt/custom/revoke', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      token: sessionToken,
      reason: "user_logout"
    })
  });

  if (response.ok) {
    // Token successfully revoked
    redirectToLogin();
  } else {
    // Handle revocation error
    console.error('Logout failed');
  }
}
```

### 2. Security Incident Response
```bash
# Emergency: Revoke compromised admin token
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d '{"token": "COMPROMISED_ADMIN_TOKEN", "reason": "security_incident"}'
```

### 3. Service Deactivation
```javascript
// Revoke service tokens when deactivating a service account
async function deactivateService(serviceTokens) {
  for (const token of serviceTokens) {
    await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token })
    });
  }
}
```

### 4. Token Replacement
```javascript
// Revoke old token when issuing a new one
async function refreshToken(oldToken) {
  // Generate new token first
  const newToken = await generateNewToken();

  // Revoke old token
  await fetch('/jwt/custom/revoke', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token: oldToken })
  });

  return newToken;
}
```

## Testing

### Manual Testing with curl

#### Test 1: Successful Revocation
```bash
# Step 1: Generate a test token
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "TEST_REVOKE", "content": {"sub": "test-user"}, "expirationInMinutes": 60}' \
  | jq -r '.token')

# Step 2: Verify token is valid
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\"}"
# Expected: true

# Step 3: Revoke the token with reason
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\", \"reason\": \"testing\"}"
# Expected: HTTP 200 - {"status":"revoked","message":"Token has been successfully revoked"}

# Step 3b: Try to revoke the same token again (idempotency test)
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\", \"reason\": \"testing_again\"}"
# Expected: HTTP 409 - {"status":"already_revoked","message":"Token was already revoked"}

# Step 4: Verify token is now invalid
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\"}"
# Expected: false
```

#### Test 2: Missing Token Error
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d '{}'
# Expected: {"error":"invalid_request","message":"Token is required"}
```

#### Test 3: Invalid Token Error
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d '{"token": "invalid.token.format"}'
# Expected: {"error":"revocation_failed","message":"Failed to revoke token: ..."}
```

#### Test 4: Empty Token Error
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke \
  -H "Content-Type: application/json" \
  -d '{"token": ""}'
# Expected: {"error":"invalid_request","message":"Token is required"}
```

### Automated Testing

**Test Suite Example:**
```javascript
describe('JWT Revocation Endpoint', () => {
  let validToken;

  beforeEach(async () => {
    // Generate a fresh token for each test
    validToken = await generateTestToken();
  });

  test('successfully revokes valid token', async () => {
    const response = await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: validToken })
    });

    expect(response.status).toBe(200);

    const result = await response.json();
    expect(result.status).toBe('revoked');

    // Verify token is now invalid
    const validation = await validateToken(validToken);
    expect(validation.valid).toBe(false);
  });

  test('returns error for missing token', async () => {
    const response = await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });

    expect(response.status).toBe(400);

    const result = await response.json();
    expect(result.error).toBe('invalid_request');
  });

  test('returns error for invalid token', async () => {
    const response = await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: 'invalid.token' })
    });

    expect(response.status).toBe(400);

    const result = await response.json();
    expect(result.error).toBe('revocation_failed');
  });

  test('handles double revocation correctly', async () => {
    // First revocation
    const firstResponse = await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        token: validToken,
        reason: "first_revocation"
      })
    });

    expect(firstResponse.status).toBe(200);
    const firstResult = await firstResponse.json();
    expect(firstResult.status).toBe('revoked');

    // Second revocation of same token
    const secondResponse = await fetch('/jwt/custom/revoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        token: validToken,
        reason: "second_revocation"
      })
    });

    // Should return 409 Conflict
    expect(secondResponse.status).toBe(409);
    const secondResult = await secondResponse.json();
    expect(secondResult.status).toBe('already_revoked');
  });
});
```

## Error Handling

### Common Error Scenarios

**1. Malformed Request**
```json
// Request
{}

// Response (400)
{
  "error": "invalid_request",
  "message": "Token is required"
}
```

**2. Invalid Token Format**
```json
// Request
{"token": "not.a.jwt"}

// Response (400)
{
  "error": "revocation_failed",
  "message": "Failed to revoke token: Invalid token format"
}
```

**3. Expired Token Revocation**
```json
// Request
{"token": "eyJ...EXPIRED_TOKEN"}

// Response (200) - Still succeeds
{
  "status": "revoked",
  "message": "Token has been successfully revoked"
}
```

**Note**: Revoking expired tokens succeeds because:
- Prevents replay attacks with expired tokens
- Maintains consistent audit trail
- Simplifies client logic (no need to check expiration before revocation)

### Error Response Format

All error responses follow this format:
```json
{
  "error": "error_code",
  "message": "Human-readable error description"
}
```

**Error Codes:**
- `invalid_request`: Missing or empty token field
- `revocation_failed`: Token parsing or database operation failed

## Security Considerations

### Token Security

**Revocation Authority:**
- Any client with access to a token can revoke it
- No additional authentication required for revocation
- Consider implementing authorization checks for sensitive tokens

**Information Disclosure:**
- Revocation endpoint doesn't reveal whether token was previously valid
- Error messages are generic to prevent information leakage
- Database operations are logged for audit purposes

### Best Practices

**1. Immediate Revocation:**
```javascript
// Revoke token immediately on security events
async function handleSecurityIncident(userTokens) {
  // Don't wait - revoke all tokens immediately
  await Promise.all(
    userTokens.map(token => revokeToken(token))
  );
}
```

**2. Graceful Error Handling:**
```javascript
async function safeRevoke(token) {
  try {
    await revokeToken(token);
    return { success: true };
  } catch (error) {
    // Log error but don't throw - revocation is best-effort
    console.warn('Token revocation failed:', error.message);
    return { success: false, error: error.message };
  }
}
```

**3. Batch Revocation:**
```javascript
// For multiple tokens, use sequential processing to avoid overwhelming the system
async function revokeMultipleTokens(tokens) {
  const results = [];
  for (const token of tokens) {
    const result = await safeRevoke(token);
    results.push(result);

    // Brief delay to prevent database overload
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  return results;
}
```

## Database Impact

### Denylist Table Structure

**Table: `custom.denylist`**
```sql
CREATE TABLE custom.denylist (
    jwt_uuid      UUID PRIMARY KEY,                    -- JWT's 'jti' claim
    denylisted_at TIMESTAMP NOT NULL DEFAULT now(),    -- Revocation timestamp
    expires_at    TIMESTAMP NOT NULL,                  -- Original expiration
    reason        TEXT                                 -- Optional revocation reason
);
```

### Storage Growth

**Denylist Growth Pattern:**
- One entry per revoked token
- Entries cleaned up after token expiration
- Storage grows with revocation rate, not total token issuance

**Cleanup Automation:**
```sql
-- Automatic cleanup of expired denylist entries
DELETE FROM custom.denylist
WHERE expires_at < NOW();
```

### Performance Impact

**Query Performance:**
- Revocation: Single INSERT operation (very fast)
- Validation: Single PRIMARY KEY lookup (very fast)
- Cleanup: Batch DELETE with index on `expires_at`

**Estimated Performance:**
- **Revocation**: < 10ms (single database insert)
- **Validation Check**: < 5ms (primary key lookup)
- **Cleanup**: Depends on denylist size, typically < 100ms

## Integration Patterns

### Frontend Integration

**React Example:**
```javascript
import { useAuth } from './auth-context';

function LogoutButton() {
  const { token, logout } = useAuth();

  const handleLogout = async () => {
    try {
      // Revoke token on server
      await fetch('/jwt/custom/revoke', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token })
      });
    } catch (error) {
      console.warn('Token revocation failed:', error);
      // Continue with logout anyway
    } finally {
      // Clear local session regardless
      logout();
    }
  };

  return <button onClick={handleLogout}>Logout</button>;
}
```

### Backend Integration

**Express.js Middleware:**
```javascript
const revokeToken = async (req, res, next) => {
  const token = req.headers.authorization?.replace('Bearer ', '');

  if (token && req.method === 'POST' && req.path === '/logout') {
    try {
      await fetch('http://tim:8085/jwt/custom/revoke', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token })
      });
    } catch (error) {
      console.warn('Token revocation failed:', error.message);
    }
  }

  next();
};
```

### Microservices Integration

**Service-to-Service Revocation:**
```python
import requests
import logging

class TokenManager:
    def __init__(self, tim_url="http://tim:8085"):
        self.tim_url = tim_url

    def revoke_token(self, token):
        """Revoke a JWT token"""
        try:
            response = requests.post(
                f"{self.tim_url}/jwt/custom/revoke",
                json={"token": token},
                timeout=5
            )

            if response.status_code == 200:
                logging.info(f"Token revoked successfully")
                return True
            else:
                logging.warning(f"Token revocation failed: {response.text}")
                return False

        except requests.RequestException as e:
            logging.error(f"Token revocation error: {e}")
            return False

    def revoke_user_tokens(self, user_id):
        """Revoke all tokens for a user (requires token listing endpoint)"""
        # This would require /jwt/custom/list endpoint
        pass
```

## Monitoring and Observability

### Key Metrics

**Revocation Metrics:**
- Revocation rate (tokens/hour)
- Revocation success rate (%)
- Average revocation response time
- Denylist growth rate

**Alert Thresholds:**
- High revocation rate (potential security incident)
- Low revocation success rate (system issues)
- Large denylist size (cleanup issues)

### Logging Examples

**Successful Revocation:**
```
INFO [2025-09-28T21:45:30Z] JWT revoked successfully - jti=7f72744d-b8d9-406e-bb93-f64c093c7b91 duration=8ms
```

**Failed Revocation:**
```
WARN [2025-09-28T21:45:30Z] JWT revocation failed - reason="Invalid token format" duration=2ms
```

**Cleanup Operation:**
```
INFO [2025-09-28T22:00:00Z] Denylist cleanup completed - removed=142 entries duration=45ms
```

### Monitoring Dashboard

**Key Dashboard Widgets:**
1. **Revocation Rate**: Tokens revoked per hour
2. **Success Rate**: Percentage of successful revocations
3. **Response Time**: Average API response time
4. **Denylist Size**: Current number of revoked tokens
5. **Cleanup Efficiency**: Expired entries removed per cleanup cycle

## Troubleshooting

### Common Issues

**1. High Revocation Rate**
```bash
# Check recent revocations
docker exec tim-postgres psql -U tim -d tim -c "
SELECT DATE(denylisted_at), COUNT(*)
FROM custom.denylist
WHERE denylisted_at > NOW() - INTERVAL '24 hours'
GROUP BY DATE(denylisted_at);"
```

**2. Revocation Failures**
```bash
# Check application logs
docker logs tim | grep -i "revocation failed"

# Check database connectivity
docker exec tim-postgres psql -U tim -d tim -c "SELECT 1;"
```

**3. Large Denylist**
```bash
# Check denylist size
docker exec tim-postgres psql -U tim -d tim -c "
SELECT
  COUNT(*) as total_revoked,
  COUNT(*) FILTER (WHERE expires_at < NOW()) as expired_entries,
  COUNT(*) FILTER (WHERE expires_at >= NOW()) as active_entries
FROM custom.denylist;"
```

### Recovery Procedures

**Manual Cleanup:**
```sql
-- Emergency denylist cleanup
DELETE FROM custom.denylist WHERE expires_at < NOW() - INTERVAL '1 day';

-- Check cleanup results
SELECT COUNT(*) FROM custom.denylist;
```

**Reset Denylist (Emergency Only):**
```sql
-- WARNING: This removes all revoked tokens
-- ONLY use in emergency situations
TRUNCATE TABLE custom.denylist;
```

## Related Documentation

- **[JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**: How revocation affects validation
- **[JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**: Token creation for testing
- **[Database Operations](./databases/06-operations.md)**: Denylist maintenance procedures
- **[HTTP Response Codes](./09-http-response-codes.md)**: Complete status code reference

## Future Enhancements

### Planned Features

**1. Bulk Revocation Endpoint:**
```
POST /jwt/custom/bulk-revoke
{
  "tokens": ["token1", "token2", "token3"],
  "reason": "Security incident"
}
```

**2. Token Listing for Revocation:**
```
GET /jwt/custom/list?user=user123
POST /jwt/custom/revoke-user
{"user": "user123", "reason": "Account deactivation"}
```

**3. Enhanced Revocation Metadata:**
```json
{
  "token": "jwt_token_here",
  "reason": "user_logout",
  "metadata": {
    "user_id": "user123",
    "session_id": "sess_456"
  }
}
```

**Note**: Reason field is now implemented and stored in `custom.denylist.reason` column.

**4. Revocation Notifications:**
- Webhook notifications for revocation events
- Real-time revocation propagation to other services
- Audit log integration