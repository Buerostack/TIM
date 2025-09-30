# TIM 2.0 - Token Identity Manager
*Advanced JWT & OAuth2 Integration Platform*

## Overview
TIM 2.0 (Token Identity Manager) is a comprehensive authentication and token management service that provides OAuth2/OIDC authentication flows and custom JWT token management capabilities.

**Evolution Note**: TIM 2.0 represents a complete architectural rewrite, evolving from the original TARA Integration Module to become a universal, provider-agnostic JWT and OAuth2 platform.

## Quick Start
1. **Start the service**: `docker-compose up -d`
2. **Access Swagger UI**: http://localhost:8085
3. **Generate a test token**: Use `/jwt/custom/generate` endpoint
4. **Test endpoints**: Use the generated Bearer token for authentication

*TIM 2.0 maintains full backward compatibility while introducing enhanced JWT management and provider-agnostic OAuth2 capabilities.*

## Documentation Structure

### 📚 API Documentation
- **[OpenAPI Specification](api/openapi.yaml)** - Complete API specification with examples
- **[Interactive Swagger UI](http://localhost:8085)** - Test endpoints with live documentation

### 🔗 Endpoint Guides
- **[OAuth2/OIDC Authentication](endpoints/oauth2-authentication.md)** - Provider discovery, login flows, session validation
- **[Custom JWT Management](endpoints/custom-jwt-management.md)** - Generate, list, extend, revoke, and validate JWT tokens
- **[Public Keys](endpoints/public-keys.md)** - JWT signature verification keys (JWKS format)

### 🗄️ Database
- **[Database Schema](database/schema.md)** - Complete schema documentation with indexes and relationships


## Core Features

### 🔐 Authentication Methods
- **OAuth2/OIDC Providers**: Google, GitHub, and custom providers
- **Custom JWT Tokens**: Self-managed tokens with configurable claims
- **Session Management**: Secure cookie-based sessions

### 🎫 JWT Token Management
- **Generation**: Create tokens with custom claims and expiration
- **Listing**: View all tokens owned by authenticated user
- **Extension**: Extend token expiration times
- **Revocation**: Securely invalidate tokens with audit trail
- **Validation**: Verify token signature and status

### 🔒 Security Features
- **RSA256 Signatures**: Industry-standard JWT signing
- **Bearer Token Authentication**: Secure API access
- **Token Revocation**: Immediate invalidation capability
- **PKCE Support**: Enhanced OAuth2 security
- **CSRF Protection**: State parameter validation

## Architecture

### Service Components
```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   OAuth2/OIDC   │  │   Custom JWT    │  │   Public Keys   │
│  Authentication │  │   Management    │  │   (JWKS)        │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • Provider      │  │ • Generate      │  │ • Signature     │
│   Discovery     │  │ • List/Filter   │  │   Verification  │
│ • Login Flows   │  │ • Extend        │  │ • Key Rotation  │
│ • Session Mgmt  │  │ • Revoke        │  │ • JWKS Format   │
│ • Validation    │  │ • Validate      │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Database Schemas
- **custom**: JWT metadata, revocation lists
- **tara**: OAuth2 tokens, state management

## Development

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- PostgreSQL (containerized)

### Local Setup
```bash
# Clone and start services
git clone <repository>
cd TIM
docker-compose up -d

# Access services
curl http://localhost:8085/auth/health
open http://localhost:8085  # Swagger UI
```

### Testing Workflows

#### 1. OAuth2 Authentication
```bash
# 1. Check available providers
curl http://localhost:8085/auth/providers

# 2. Start login (browser redirect)
open http://localhost:8085/auth/login/google

# 3. Validate session after callback
curl -b cookies.txt http://localhost:8085/auth/validate
```

#### 2. Custom JWT Management
```bash
# 1. Generate token
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName":"test-token","content":{"sub":"user123"},"expirationInMinutes":60}'

# 2. List tokens (requires Bearer auth)
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json"

# 3. Validate token
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d '{"token":"<jwt-token>"}'
```

## Configuration

### Environment Variables
- `KEY_PASS`: Private key password (default: "changeme")
- `DATABASE_URL`: PostgreSQL connection string
- `OAUTH2_PROVIDERS`: JSON configuration for OAuth2 providers

### Security Configuration
- All `/jwt/**` endpoints: Public access
- All `/auth/**` endpoints: Public access (session-based)
- Static resources: Public access
- CSRF disabled for API endpoints

## Monitoring & Maintenance

### Health Checks
- **OAuth2 Service**: `GET /auth/health`
- **Database**: Connection validated on startup
- **JWT Keys**: Automatically generated if missing

### Database Maintenance
```sql
-- Clean expired revoked tokens
DELETE FROM custom.denylist WHERE expires_at < now();
DELETE FROM tara.denylist WHERE expires_at < now();

-- Clean expired OAuth2 states
DELETE FROM tara.oauth_state WHERE created_at < now() - interval '1 hour';
```

### Performance Tuning
- Database indexes on frequently queried fields
- JWT validation caching recommendations
- Key rotation procedures

## Integration Examples

### Frontend Integration
```javascript
// Generate and use JWT token
const response = await fetch('/jwt/custom/generate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    JWTName: 'frontend-token',
    content: { sub: 'user123', role: 'user' },
    expirationInMinutes: 60
  })
});
const { token } = await response.json();

// Use token for authenticated requests
const apiResponse = await fetch('/jwt/custom/list/me', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

### External Service Integration
```java
// Validate TIM tokens in external service
JwkProvider provider = new UrlJwkProvider("http://tim:8085/jwt/keys/public");
DecodedJWT jwt = JWT.decode(token);
RSAPublicKey publicKey = (RSAPublicKey) provider.get("jwtsign").getPublicKey();
Algorithm.RSA256(publicKey, null).verify(jwt);
```

## Support
- **Documentation**: Complete API specification in OpenAPI format
- **Interactive Testing**: Swagger UI with authentication examples
- **Database Schema**: Detailed schema documentation with examples