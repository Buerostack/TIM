# JWT Validation Endpoint

## Overview

The JWT validation endpoints provide token verification services with two response formats:

- **`/jwt/custom/validate`**: Full JSON response with detailed token information
- **`/jwt/custom/validate/boolean`**: Simple TRUE/FALSE text response for lightweight checks

**📋 For generation endpoint details, see: [JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**
**📋 For audience configuration details, see: [JWT Audience Configuration](./08-jwt-audience-configuration.md)**

## API Reference

### JWT Validation Endpoint
```
POST /jwt/custom/validate
```

**Content-Type:** `application/json`

### JWT Boolean Validation Endpoint
```
POST /jwt/custom/validate/boolean
```

**Content-Type:** `application/json`
**Response Type:** `text/plain`

**Request Body:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ...",
  "audience": "tim-audience"
}
```

**Parameters:**
- `token` (string, required): The JWT token to validate
- `audience` (string, optional): Expected audience for validation. If provided, the token's audience claim must match this value.

### Response

**Content-Type:** `application/json`

#### Success Response (Valid Token)
```json
{
  "valid": true,
  "active": true,
  "reason": "Valid",
  "subject": "user123",
  "issuer": "TIM",
  "audience": "tim-audience",
  "expires_at": "2025-09-28T19:44:55Z",
  "issued_at": "2025-09-28T18:44:55Z",
  "jwt_id": "d9e09c4f-e0e5-4bf5-85ef-7a01746b5a2a",
  "claims": {
    "aud": ["tim-audience"],
    "sub": "user123",
    "role": "admin",
    "iss": "TIM",
    "exp": "2025-09-28T19:44:55.000+00:00",
    "iat": "2025-09-28T18:44:55.000+00:00",
    "jti": "d9e09c4f-e0e5-4bf5-85ef-7a01746b5a2a"
  }
}
```

#### Error Response (Invalid Token)
```json
{
  "valid": false,
  "active": false,
  "reason": "Invalid signature",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

### Boolean Validation Response

The boolean endpoint returns simple text responses:

#### Valid Token Response
```
TRUE
```

#### Invalid Token Response
```
FALSE
```

**Response Headers:**
- `Content-Type: text/plain`
- HTTP Status: 200 (for valid tokens), 400 (for invalid tokens)

## Use Cases

### Standard Validation Endpoint
- **API integrations** requiring detailed token information
- **Debugging and logging** with comprehensive validation details
- **Token introspection** for extracting claims and metadata
- **Error analysis** with specific failure reasons

### Boolean Validation Endpoint
- **High-performance services** needing minimal response overhead
- **Load balancers** and **API gateways** for quick token checks
- **Microservices** requiring fast authorization decisions
- **Shell scripts** and **automation tools** for simple validation
- **Mobile applications** with bandwidth constraints
- **Monitoring systems** for token validity checks

## Comparison

| Feature | Standard `/validate` | Boolean `/validate/boolean` |
|---------|---------------------|----------------------------|
| **Response Format** | JSON | Plain text |
| **Response Size** | ~400-500 bytes | 4-5 bytes |
| **Content-Type** | `application/json` | `text/plain` |
| **Valid Token Response** | Detailed JSON object | `TRUE` |
| **Invalid Token Response** | Error JSON with reason | `FALSE` |
| **Performance** | Standard | Optimized |
| **Use Case** | Full token analysis | Quick validation |
| **Claims Extraction** | ✅ Yes | ❌ No |
| **Error Details** | ✅ Yes | ❌ No |
| **Bandwidth Usage** | Higher | Minimal |

**Response Fields:**
- `valid` (boolean): Whether the token is valid
- `active` (boolean): Whether the token is active (not expired and not revoked)
- `reason` (string): Human-readable validation result or error message
- `subject` (string): Token subject (sub claim) if valid
- `issuer` (string): Token issuer (iss claim) if valid
- `audience` (string): Token audience (aud claim) if valid
- `expires_at` (string): Token expiration time in ISO 8601 format if valid
- `issued_at` (string): Token issuance time in ISO 8601 format if valid
- `jwt_id` (string): Unique token identifier (jti claim) if valid
- `claims` (object): All token claims if valid

## Validation Criteria

The endpoint performs the following validations in order:

1. **Signature Verification**: Verifies the JWT was signed by TIM's private key
2. **Expiration Check**: Ensures the token has not expired
3. **Revocation Check**: Checks if the token has been denylisted
4. **Audience Check**: If audience parameter is provided, validates it matches the token's audience claim

## Common Error Reasons

- `"Invalid signature"`: Token was not signed by TIM or signature is corrupted
- `"Token expired"`: Token expiration time has passed
- `"Token revoked"`: Token has been added to the denylist
- `"Invalid audience"`: Token audience doesn't match expected audience
- `"Parse error: [details]"`: Token format is invalid or malformed

## Data Flow

```
Client Request → CustomJwtController.validate()
                     ↓
               CustomJwtService.validate()
                     ↓
            [Parse JWT & Extract Claims]
                     ↓
            [Verify Signature via JwtSignerService]
                     ↓
            [Check Expiration]
                     ↓
            [Check Revocation Status via Database]
                     ↓
            [Validate Audience if provided]
                     ↓
               Return JwtValidationResponse
```

## Source Code Components

### Controller Layer
- **File**: `app/custom-jwt/src/main/java/buerostack/jwt/api/CustomJwtController.java:73-86`
- **Method**: `validate(@RequestBody JwtValidationRequest request)`
- **Responsibility**: HTTP request handling, JSON response formatting

- **File**: `app/custom-jwt/src/main/java/buerostack/jwt/api/CustomJwtController.java:88-106`
- **Method**: `validateBoolean(@RequestBody JwtValidationRequest request)`
- **Responsibility**: HTTP request handling, boolean response formatting

### Service Layer
- **File**: `app/custom-jwt/src/main/java/buerostack/jwt/service/CustomJwtService.java:24-58`
- **Method**: `validate(String token, String expectedAudience)`
- **Responsibility**: JWT validation logic, claims extraction

### DTOs
- **Request**: `app/custom-jwt/src/main/java/buerostack/jwt/api/JwtValidationRequest.java`
- **Response**: `app/custom-jwt/src/main/java/buerostack/jwt/api/JwtValidationResponse.java`

### Dependencies
- **JwtSignerService**: `app/common/src/main/java/buerostack/config/JwtSignerService.java:18`
  - Provides `verify(String token)` method for signature validation
- **CustomDenylistRepo**: Database repository for checking token revocation status

## Testing

### Manual Testing with curl

**Note:** The tokens in these examples expire after 1 hour. If you get "Token expired" errors, generate a new token using the generation endpoint first.

**Generate a fresh token:**
```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "TEST_TOKEN", "content": {"sub": "user123", "role": "admin"}, "expirationInMinutes": 60, "setCookie": false}'
```

#### Standard Validation Endpoint

```bash
# Test 1: Basic validation
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MDk1MzIzLCJpYXQiOjE3NTkwOTE3MjMsImp0aSI6IjRiMmJjMTRjLWMyMzQtNGY2My1hNGRkLTQ1MjI4NjBkOWIzNiJ9.cO-RBsaJw7bcr_YgCdhddkya_Bp6AoVqgBHyLiW-5vGYpmWXsu3U0_d5IsnEGk8T1yTQ-kXzVFyRLIzjHLkpguQdiTYeiyjFap4inqggGxs8fFKEW2yd_bJknHbMj886nM5o1XmNnNSlxTuO1wEdnLYFu5kov3srEsiwjxFxs3kHCAsmr_7Qe2eFvLXJUOen_iLK0y9qdf8g6XaAe8sMtwnSho3tkG5gjYZZxUeYXDPAMECXyTJLlVOe2lEersCrqKJI5nkb5TYLvOICMGELrVLH-V5U6hf8defqKsk4BB72XfwXmrcrdmZi2LvCXkNk0nX03TS_vo1mrZ4AI3In_w"}'

# Test 2: Validation with audience check
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MDk1MzIzLCJpYXQiOjE3NTkwOTE3MjMsImp0aSI6IjRiMmJjMTRjLWMyMzQtNGY2My1hNGRkLTQ1MjI4NjBkOWIzNiJ9.cO-RBsaJw7bcr_YgCdhddkya_Bp6AoVqgBHyLiW-5vGYpmWXsu3U0_d5IsnEGk8T1yTQ-kXzVFyRLIzjHLkpguQdiTYeiyjFap4inqggGxs8fFKEW2yd_bJknHbMj886nM5o1XmNnNSlxTuO1wEdnLYFu5kov3srEsiwjxFxs3kHCAsmr_7Qe2eFvLXJUOen_iLK0y9qdf8g6XaAe8sMtwnSho3tkG5gjYZZxUeYXDPAMECXyTJLlVOe2lEersCrqKJI5nkb5TYLvOICMGELrVLH-V5U6hf8defqKsk4BB72XfwXmrcrdmZi2LvCXkNk0nX03TS_vo1mrZ4AI3In_w", "audience": "tim-audience"}'

# Test 3: Invalid token
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "invalid.token.here"}'
```

#### Boolean Validation Endpoint

```bash
# Test 1: Boolean validation - valid token
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MDk1MzIzLCJpYXQiOjE3NTkwOTE3MjMsImp0aSI6IjRiMmJjMTRjLWMyMzQtNGY2My1hNGRkLTQ1MjI4NjBkOWIzNiJ9.cO-RBsaJw7bcr_YgCdhddkya_Bp6AoVqgBHyLiW-5vGYpmWXsu3U0_d5IsnEGk8T1yTQ-kXzVFyRLIzjHLkpguQdiTYeiyjFap4inqggGxs8fFKEW2yd_bJknHbMj886nM5o1XmNnNSlxTuO1wEdnLYFu5kov3srEsiwjxFxs3kHCAsmr_7Qe2eFvLXJUOen_iLK0y9qdf8g6XaAe8sMtwnSho3tkG5gjYZZxUeYXDPAMECXyTJLlVOe2lEersCrqKJI5nkb5TYLvOICMGELrVLH-V5U6hf8defqKsk4BB72XfwXmrcrdmZi2LvCXkNk0nX03TS_vo1mrZ4AI3In_w"}'
# Response: TRUE

# Test 2: Boolean validation with audience
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MDk1MzIzLCJpYXQiOjE3NTkwOTE3MjMsImp0aSI6IjRiMmJjMTRjLWMyMzQtNGY2My1hNGRkLTQ1MjI4NjBkOWIzNiJ9.cO-RBsaJw7bcr_YgCdhddkya_Bp6AoVqgBHyLiW-5vGYpmWXsu3U0_d5IsnEGk8T1yTQ-kXzVFyRLIzjHLkpguQdiTYeiyjFap4inqggGxs8fFKEW2yd_bJknHbMj886nM5o1XmNnNSlxTuO1wEdnLYFu5kov3srEsiwjxFxs3kHCAsmr_7Qe2eFvLXJUOen_iLK0y9qdf8g6XaAe8sMtwnSho3tkG5gjYZZxUeYXDPAMECXyTJLlVOe2lEersCrqKJI5nkb5TYLvOICMGELrVLH-V5U6hf8defqKsk4BB72XfwXmrcrdmZi2LvCXkNk0nX03TS_vo1mrZ4AI3In_w", "audience": "tim-audience"}'
# Response: TRUE

# Test 3: Boolean validation - invalid token
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d '{"token": "invalid.token.here"}'
# Response: FALSE

# Test 4: Boolean validation with issuer
curl -X POST http://localhost:8085/jwt/custom/validate/boolean \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MDk1MzIzLCJpYXQiOjE3NTkwOTE3MjMsImp0aSI6IjRiMmJjMTRjLWMyMzQtNGY2My1hNGRkLTQ1MjI4NjBkOWIzNiJ9.cO-RBsaJw7bcr_YgCdhddkya_Bp6AoVqgBHyLiW-5vGYpmWXsu3U0_d5IsnEGk8T1yTQ-kXzVFyRLIzjHLkpguQdiTYeiyjFap4inqggGxs8fFKEW2yd_bJknHbMj886nM5o1XmNnNSlxTuO1wEdnLYFu5kov3srEsiwjxFxs3kHCAsmr_7Qe2eFvLXJUOen_iLK0y9qdf8g6XaAe8sMtwnSho3tkG5gjYZZxUeYXDPAMECXyTJLlVOe2lEersCrqKJI5nkb5TYLvOICMGELrVLH-V5U6hf8defqKsk4BB72XfwXmrcrdmZi2LvCXkNk0nX03TS_vo1mrZ4AI3In_w", "issuer": "TIM"}'
# Response: TRUE
```

**📋 For comprehensive testing examples including generation and multi-audience scenarios, see: [JWT Audience Configuration](./08-jwt-audience-configuration.md#testing)**

### Expected Test Results

- **Valid token with correct audience**: `valid: true, active: true, reason: "Valid"`
- **Valid token without audience**: `valid: true, active: true, reason: "Valid"`
- **Invalid token**: `valid: false, active: false, reason: "Parse error: [details]"`
- **Wrong audience**: `valid: false, active: false, reason: "Invalid audience"`
- **Expired token**: `valid: false, active: false, reason: "Token expired"`
- **Revoked token**: `valid: false, active: false, reason: "Token revoked"`

## Security Considerations

- The endpoint validates JWT signatures using RSA-256 with TIM's keystore
- All validation is performed server-side with no client-side verification
- Revoked tokens are checked against the database denylist
- Audience validation prevents token misuse across different services
- Error messages provide sufficient detail for debugging while avoiding information leakage

## Standards Compliance

This endpoint follows JWT validation best practices:
- **RFC 7519**: JSON Web Token (JWT) standard
- **RFC 7515**: JSON Web Signature (JWS) for signature verification
- **RFC 7517**: JSON Web Key (JWK) for key management
- Returns introspection-style response format similar to RFC 7662 (OAuth 2.0 Token Introspection)

## Related Documentation

- **[JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**: Token generation API with custom claims
- **[JWT Audience Configuration](./08-jwt-audience-configuration.md)**: Comprehensive audience setup, security, and testing examples