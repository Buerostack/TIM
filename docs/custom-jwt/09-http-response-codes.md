# TIM HTTP Response Codes

## Overview

TIM follows RESTful API conventions and returns appropriate HTTP status codes to indicate the result of each request. This document provides a comprehensive guide to all HTTP response codes used by TIM endpoints.

## HTTP Status Code Standards

TIM implements HTTP response codes according to RFC 7231 (HTTP/1.1 Semantics and Content) and REST API best practices:

- **2xx Success**: Request succeeded
- **4xx Client Error**: Request contains bad syntax or cannot be fulfilled
- **5xx Server Error**: Server failed to fulfill an apparently valid request

## JWT Custom Endpoints

### `/jwt/custom/generate` - Token Generation

#### Success Responses

**`200 OK`** - Token successfully generated
```json
{
  "status": "created",
  "name": "API_TOKEN",
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "expiresAt": "2025-09-28T22:37:09Z"
}
```

#### Error Responses

**`400 Bad Request`** - Invalid request parameters
```json
{
  "error": "invalid_audience",
  "message": "One or more requested audiences are not allowed",
  "allowed_audiences": ["payment-service", "user-service"]
}
```

**Common 400 Error Scenarios:**
- Missing required fields (`JWTName`, `content`, `expirationInMinutes`)
- Invalid audience when validation is enabled
- Malformed JSON in request body
- Invalid expiration time (e.g., negative minutes)

### `/jwt/custom/validate` - Token Validation

#### Success Responses

**`200 OK`** - Token is valid and active
```json
{
  "valid": true,
  "active": true,
  "reason": "Valid",
  "subject": "user123",
  "issuer": "TIM",
  "audience": "tim-audience",
  "expires_at": "2025-09-28T22:37:09Z",
  "issued_at": "2025-09-28T21:37:09Z",
  "jwt_id": "7f72744d-b8d9-406e-bb93-f64c093c7b91",
  "claims": {
    "aud": ["tim-audience"],
    "sub": "user123",
    "role": "admin",
    "iss": "TIM",
    "exp": "2025-09-28T22:37:09.000+00:00",
    "iat": "2025-09-28T21:37:09.000+00:00",
    "jti": "7f72744d-b8d9-406e-bb93-f64c093c7b91"
  }
}
```

#### Error Responses

**`400 Bad Request`** - Invalid request format
```json
{
  "valid": false,
  "active": false,
  "reason": "Token is required",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

**`401 Unauthorized`** - Token validation failed
```json
{
  "valid": false,
  "active": false,
  "reason": "Token expired",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

**Common 401 Error Reasons:**
- `"Invalid signature"` - Token signature verification failed
- `"Token expired"` - Token expiration time has passed
- `"Token revoked"` - Token has been added to denylist
- `"Invalid audience"` - Token audience doesn't match expected value
- `"Invalid issuer"` - Token issuer doesn't match expected value
- `"Invalid token format"` - Token is malformed or unparseable
- `"Invalid token"` - Generic parsing or validation error

### `/jwt/custom/validate/boolean` - Boolean Validation

#### Success Responses

**`200 OK`** - Token is valid and active
```
Content-Type: text/plain

true
```

#### Error Responses

**`400 Bad Request`** - Invalid request format
```
Content-Type: text/plain

false
```

**`401 Unauthorized`** - Token validation failed
```
Content-Type: text/plain

false
```

**Response Logic:**
- **Valid + Active Token**: `200 OK` + `"true"`
- **Invalid/Expired/Revoked Token**: `401 Unauthorized` + `"false"`
- **Malformed Request**: `400 Bad Request` + `"false"`

### `/jwt/custom/revoke` - Token Revocation

#### Success Responses

**`200 OK`** - Token successfully revoked
```json
{
  "status": "revoked",
  "message": "Token has been successfully revoked"
}
```

#### Error Responses

**`400 Bad Request`** - Invalid request or revocation failed
```json
{
  "error": "invalid_request",
  "message": "Token is required"
}
```

```json
{
  "error": "revocation_failed",
  "message": "Failed to revoke token: Invalid token format"
}
```

**Common 400 Error Scenarios:**
- Missing token in request body
- Malformed or invalid token format
- Database errors during revocation
- Token already revoked (duplicate revocation)

## HTTP Response Headers

### Standard Headers

All TIM responses include standard HTTP headers:

```http
Content-Type: application/json
Date: Sat, 28 Sep 2025 21:37:09 GMT
Transfer-Encoding: chunked
```

### Custom Headers

**Boolean Validation Endpoint:**
```http
Content-Type: text/plain
```

**Token Generation with Cookies:**
```http
Set-Cookie: API_TOKEN=eyJraWQiOiJqd3RzaWduIi...; Path=/; HttpOnly
```

## Error Response Format

### JSON Endpoints Error Format

**Validation Endpoints (`/validate`, `/validate/boolean`):**
```json
{
  "valid": false,
  "active": false,
  "reason": "Specific error message",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

**Other Endpoints:**
```json
{
  "error": "error_code",
  "message": "Human-readable error description"
}
```

### Text Endpoints Error Format

**Boolean Validation (`/validate/boolean`):**
```
false
```

## Status Code Decision Matrix

| Scenario | Generation | Validation | Boolean | Revocation |
|----------|------------|------------|---------|------------|
| **Success** | `200 OK` | `200 OK` | `200 OK` | `200 OK` |
| **Valid Request, Invalid Token** | N/A | `401 Unauthorized` | `401 Unauthorized` | `400 Bad Request` |
| **Missing Required Fields** | `400 Bad Request` | `400 Bad Request` | `400 Bad Request` | `400 Bad Request` |
| **Invalid JSON** | `400 Bad Request` | `400 Bad Request` | `400 Bad Request` | `400 Bad Request` |
| **Invalid Audience (Generation)** | `400 Bad Request` | N/A | N/A | N/A |
| **Server Error** | `500 Internal Server Error` | `500 Internal Server Error` | `500 Internal Server Error` | `500 Internal Server Error` |

## Best Practices for API Clients

### Status Code Handling

**Client Implementation Example:**
```javascript
async function validateToken(token) {
  const response = await fetch('/jwt/custom/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token })
  });

  switch (response.status) {
    case 200:
      // Token is valid
      const result = await response.json();
      return { valid: true, data: result };

    case 401:
      // Token is invalid/expired/revoked
      const error = await response.json();
      return { valid: false, reason: error.reason };

    case 400:
      // Bad request format
      return { valid: false, reason: 'Invalid request' };

    default:
      // Server error
      return { valid: false, reason: 'Server error' };
  }
}
```

### Error Handling Strategies

**1. Distinguish Between Client and Server Errors:**
- `4xx` errors: Fix the request
- `5xx` errors: Retry or escalate

**2. Use Specific Error Messages:**
- Parse `reason` field for validation errors
- Check `error` field for operation-specific errors

**3. Implement Proper Retries:**
- Don't retry `4xx` errors (except `429 Too Many Requests`)
- Implement exponential backoff for `5xx` errors

## Security Considerations

### Status Code Information Disclosure

**Safe Status Codes:**
- `401 Unauthorized` for invalid tokens (doesn't reveal system details)
- `400 Bad Request` for malformed requests (standard HTTP behavior)

**Error Message Guidelines:**
- Generic messages for security-sensitive failures
- Detailed messages only for client-side errors
- No internal system information in public responses

### Rate Limiting (Future Enhancement)

**Planned Status Codes:**
- `429 Too Many Requests` - Rate limit exceeded
- `503 Service Unavailable` - System overload

**Rate Limiting Headers (Future):**
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
Retry-After: 60
```

## Monitoring and Observability

### Status Code Metrics

**Key Metrics to Monitor:**
- `2xx` success rate (should be > 95%)
- `4xx` client error rate (monitor for attack patterns)
- `5xx` server error rate (should be < 1%)
- Response time by status code

**Alerting Thresholds:**
- `5xx` rate > 1% for 5 minutes
- `4xx` rate > 20% for 10 minutes (potential attack)
- Average response time > 500ms

### Logging Examples

**Successful Validation:**
```
INFO [2025-09-28T21:37:09Z] JWT validation successful - jti=7f72744d-b8d9-406e-bb93-f64c093c7b91 status=200
```

**Failed Validation:**
```
WARN [2025-09-28T21:37:09Z] JWT validation failed - reason="Token expired" status=401
```

**Revocation:**
```
INFO [2025-09-28T21:37:09Z] JWT revoked - jti=7f72744d-b8d9-406e-bb93-f64c093c7b91 status=200
```

## Testing HTTP Status Codes

### Manual Testing with curl

**Test All Status Codes:**
```bash
# 200 OK - Valid token
curl -w "HTTP %{http_code}\n" -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "VALID_TOKEN_HERE"}'

# 401 Unauthorized - Invalid token
curl -w "HTTP %{http_code}\n" -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "invalid.token.here"}'

# 400 Bad Request - Missing token
curl -w "HTTP %{http_code}\n" -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{}'

# 400 Bad Request - Invalid JSON
curl -w "HTTP %{http_code}\n" -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d 'invalid json'
```

### Automated Testing

**Status Code Test Suite:**
```javascript
describe('TIM HTTP Status Codes', () => {
  test('Valid token returns 200', async () => {
    const response = await validateToken(validToken);
    expect(response.status).toBe(200);
  });

  test('Invalid token returns 401', async () => {
    const response = await validateToken('invalid.token');
    expect(response.status).toBe(401);
  });

  test('Missing token returns 400', async () => {
    const response = await validateToken('');
    expect(response.status).toBe(400);
  });
});
```

## Comparison with Industry Standards

### OAuth 2.0 Token Introspection (RFC 7662)

**TIM vs OAuth 2.0:**
- ✅ Uses `401 Unauthorized` for invalid tokens
- ✅ Returns detailed validation information
- ✅ Supports boolean response format
- ⚠️ Custom error format (not OAuth 2.0 standard)

### JWT Validation Best Practices

**Industry Alignment:**
- ✅ Clear distinction between malformed requests (400) and invalid tokens (401)
- ✅ Detailed error reasons for debugging
- ✅ Consistent response format across endpoints
- ✅ Security-conscious error messages

## Future Enhancements

### Planned Status Codes

**Additional Error Codes:**
- `429 Too Many Requests` - Rate limiting
- `503 Service Unavailable` - Maintenance mode
- `422 Unprocessable Entity` - Valid JSON but invalid semantics

**Enhanced Response Headers:**
- `WWW-Authenticate` header for 401 responses
- `Retry-After` header for 429/503 responses
- Custom `X-TIM-*` headers for debugging

---

## Related Documentation

- **[JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**: Token creation API details
- **[JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**: Token validation API details
- **[JWT Audience Configuration](./08-jwt-audience-configuration.md)**: Audience validation and error scenarios