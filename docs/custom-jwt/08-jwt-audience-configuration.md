# JWT Configuration - Issuer & Audience

## Overview

JWT issuer (`iss`) and audience (`aud`) claim configuration in TIM provides enterprise-grade token identification and scoping. This document covers JWT standards, configuration options, and security best practices for multi-instance TIM deployments.

## JWT Standards (RFC 7519)

### Issuer (`iss`) Claim

The `iss` (issuer) claim identifies **WHO issued the token**:

- **Purpose**: Specifies which TIM instance created the token
- **Set by**: TIM instance during token generation
- **Used by**: Token validators to verify token origin
- **Security benefit**: Distinguishes tokens from different TIM deployments

### Audience (`aud`) Claim

The `aud` (audience) claim identifies the **recipients** that the JWT is intended for:

- **Purpose**: Specifies WHO should accept and process the token
- **Set by**: The token issuer (TIM) when generating tokens
- **Used by**: Token recipients/validators to verify "is this token meant for me?"
- **Security benefit**: Prevents token misuse across different services

### Single vs Multiple Audiences

**Single Audience (String):**
```json
{
  "aud": "payment-service"
}
```

**Multiple Audiences (Array):**
```json
{
  "aud": ["payment-service", "user-service", "admin-dashboard"]
}
```

## TIM Configuration

### Application Properties

```properties
# JWT Custom Configuration
# Issuer identifier for this TIM instance
# IMPORTANT: Must be unique per TIM deployment for proper JWT ecosystem management
# Examples: TIM-PROD, TIM-DEV, TIM-STAGING, company-tim-prod, department-tim-dev
jwt.custom.issuer=TIM

# Enable audience validation (default: false)
jwt.custom.audience.validation.enabled=false

# Allowed audiences (comma-separated list)
# Only tokens with these audiences can be generated
# Example: jwt.custom.audience.allowed=payment-service,user-service,admin-dashboard
jwt.custom.audience.allowed=

# Default audience when none specified in generation request
# Used only when audience validation is enabled
jwt.custom.audience.default=tim-service
```

### Issuer Configuration

| Configuration | Purpose | Examples |
|---------------|---------|----------|
| **Production** | Unique per environment | `TIM-PROD`, `COMPANY-TIM-PROD` |
| **Development** | Environment identification | `TIM-DEV`, `TIM-STAGING` |
| **Multi-tenant** | Tenant identification | `CLIENT-A-TIM`, `DEPT-FINANCE-TIM` |
| **Geographic** | Regional identification | `TIM-US-EAST`, `TIM-EU-WEST` |

### Security Behavior

| Configuration | Behavior | Security Level |
|---------------|----------|----------------|
| **Issuer** | Always set per TIM instance | ✅ **Mandatory identification** |
| **Audience disabled (default)** | Any audience values accepted | ⚠️ **Warning logged at startup** |
| **Audience enabled with allowed list** | Only specified audiences permitted | ✅ **High security** |
| **Audience enabled with empty list** | All audiences permitted | ⚠️ **Medium security** |

### Configuration Rules

- **Compile-time only**: Configuration cannot be changed during runtime for security
- **Container restart required**: Changes require rebuilding and restarting containers
- **Environment variables**: Can be overridden via Docker environment variables

## API Usage

### Generation Request Examples

**No audience (backward compatible):**
```json
{
  "JWTName": "LEGACY_TOKEN",
  "content": {"sub": "user123", "role": "admin"}
  // Uses "tim-audience" when validation disabled, or default when enabled
}
```

**Single audience:**
```json
{
  "JWTName": "PAYMENT_TOKEN",
  "content": {"sub": "user456"},
  "audience": "payment-service"
}
```

**Multiple audiences:**
```json
{
  "JWTName": "MULTI_TOKEN",
  "content": {"sub": "user789"},
  "audience": ["user-service", "admin-dashboard"]
}
```

### Validation Request Examples

**Validate with audience check:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "audience": "payment-service"
}
```

**Validate with issuer check:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "issuer": "TIM-PROD"
}
```

**Validate with both audience and issuer:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIi...",
  "audience": "payment-service",
  "issuer": "TIM-PROD"
}
```

**Validate without checks:**
```json
{
  "token": "eyJraWQiOiJqd3RzaWduIi..."
  // Skips audience and issuer validation
}
```

## Error Responses

### Generation Errors

**Invalid audience when validation enabled:**
```json
{
  "error": "invalid_audience",
  "message": "One or more requested audiences are not allowed",
  "allowed_audiences": ["payment-service", "user-service"]
}
```

### Validation Errors

**Invalid audience:**
```json
{
  "valid": false,
  "active": false,
  "reason": "Invalid audience",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

**Invalid issuer:**
```json
{
  "valid": false,
  "active": false,
  "reason": "Invalid issuer",
  "subject": null,
  "issuer": null,
  "audience": null,
  "expires_at": null,
  "issued_at": null,
  "jwt_id": null,
  "claims": null
}
```

## Security Scenarios

### Multi-Instance TIM Deployment

```
🏢 Enterprise Environment:
├── TIM-PROD (iss: "COMPANY-TIM-PROD")
│   ├── Tokens for production services
│   └── High security, restricted audiences
├── TIM-DEV (iss: "COMPANY-TIM-DEV")
│   ├── Tokens for development services
│   └── Relaxed audience validation
└── TIM-CLIENT-A (iss: "CLIENT-A-TIM")
    ├── Tokens for client-specific services
    └── Client-isolated token ecosystem

Security benefit: Even if dev token is compromised,
production services reject it due to issuer mismatch.
```

### Microservices Architecture

```
🏦 Banking Application (Single TIM):
├── payment-service (aud: "payment-service")
├── user-service (aud: "user-service")
├── admin-dashboard (aud: ["user-service", "admin-dashboard"])
└── mobile-app (aud: ["payment-service", "user-service"])

Security benefit: Even if mobile token is compromised,
admin-dashboard will reject it due to audience mismatch.
```

### Token Scope Restriction

```
🔒 Production Configuration:
jwt.custom.audience.validation.enabled=true
jwt.custom.audience.allowed=payment-api,user-api,admin-api

Result: Only these 3 services can receive tokens
Prevents: Tokens for unauthorized services
```

## Configuration Examples

### Development Environment
```properties
# Relaxed for development
jwt.custom.audience.validation.enabled=false
# WARNING: "JWT Audience validation is DISABLED..." logged at startup
```

### Production Environment
```properties
# Strict validation for production
jwt.custom.audience.validation.enabled=true
jwt.custom.audience.allowed=payment-service,user-service,admin-service,mobile-api
jwt.custom.audience.default=internal-service
```

### Docker Environment Variables
```yaml
# docker-compose.yml
services:
  tim:
    environment:
      - JWT_CUSTOM_AUDIENCE_VALIDATION_ENABLED=true
      - JWT_CUSTOM_AUDIENCE_ALLOWED=payment-service,user-service
      - JWT_CUSTOM_AUDIENCE_DEFAULT=internal-service
```

## Best Practices

### 1. Enable Validation in Production
```properties
# Always enable for production environments
jwt.custom.audience.validation.enabled=true
jwt.custom.audience.allowed=service1,service2,service3
```

### 2. Use Descriptive Service Names
```
✅ Good: payment-service, user-management, admin-dashboard
❌ Bad: svc1, app, system
```

### 3. Minimize Audience Lists
```
✅ Good: ["payment-api", "billing-api"]  # Related services
❌ Bad: ["svc1", "svc2", "svc3", "svc4", "svc5"]  # Too broad
```

### 4. Monitor Startup Warnings
```
Look for: "JWT Audience validation is DISABLED. This may allow token misuse..."
Action: Enable validation or document security exception
```

### 5. Document Service Audiences
```
# Maintain a service registry
payment-service: "payment-api"
user-service: "user-api"
admin-dashboard: ["user-api", "admin-api"]
```

## Testing

### Configuration Testing

```bash
# Test disabled validation (should show warning in logs)
docker logs tim | grep "JWT Audience validation is DISABLED"

# Test enabled validation
curl -X POST http://localhost:8085/jwt/custom/generate \
  -d '{"audience": "unauthorized-service"}'
# Should return: {"error": "invalid_audience", ...}
```

### Issuer Testing

```bash
# Check current issuer configuration
docker logs tim | grep "JWT Issuer configured"

# Generate token (issuer automatically set)
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"content": {"sub": "user123"}}' | jq -r '.token')

# Validate with correct issuer
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\", \"issuer\": \"TIM\"}"
# Expected: {"valid": true, "issuer": "TIM", ...}

# Validate with wrong issuer
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\", \"issuer\": \"WRONG-ISSUER\"}"
# Expected: {"valid": false, "reason": "Invalid issuer", ...}
```

### Multi-Audience Testing

```bash
# Generate multi-audience token
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"audience": ["user-service", "admin-dashboard"]}' | jq -r '.token')

# Test validation with first audience
curl -X POST http://localhost:8085/jwt/custom/validate \
  -d "{\"token\": \"$TOKEN\", \"audience\": \"user-service\"}"
# Expected: {"valid": true, ...}

# Test validation with second audience
curl -X POST http://localhost:8085/jwt/custom/validate \
  -d "{\"token\": \"$TOKEN\", \"audience\": \"admin-dashboard\"}"
# Expected: {"valid": true, ...}

# Test validation with invalid audience
curl -X POST http://localhost:8085/jwt/custom/validate \
  -d "{\"token\": \"$TOKEN\", \"audience\": \"payment-service\"}"
# Expected: {"valid": false, "reason": "Invalid audience", ...}
```

### Combined Validation Testing

```bash
# Generate token
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"audience": "payment-service", "content": {"sub": "user456"}}' | jq -r '.token')

# Test both audience and issuer validation
curl -X POST http://localhost:8085/jwt/custom/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\", \"audience\": \"payment-service\", \"issuer\": \"TIM\"}"
# Expected: {"valid": true, "audience": "payment-service", "issuer": "TIM", ...}
```

## Troubleshooting

### Common Issues

**1. Token rejected with "Invalid audience"**
- Check if expected audience is in token's audience list
- Verify audience spelling and case sensitivity
- Use validation without audience parameter to debug

**2. Generation fails with "invalid_audience"**
- Check `jwt.custom.audience.allowed` configuration
- Verify requested audience is in allowed list
- Review startup logs for configuration errors

**3. Token rejected with "Invalid issuer"**
- Check token's issuer claim: `echo "TOKEN_PAYLOAD" | base64 -d | jq '.iss'`
- Verify expected issuer matches TIM configuration
- Review startup logs for issuer configuration

**4. Unexpected audience behavior**
- Check if validation is enabled/disabled
- Review default audience configuration
- Verify backward compatibility expectations

### Debug Commands

```bash
# Check current configuration
docker exec tim cat /opt/tim/application.properties | grep jwt.custom.audience

# View startup logs
docker logs tim | grep -i audience

# Test token content
echo "TOKEN_PAYLOAD" | base64 -d | jq '.aud'
```

## Standards Compliance

This implementation follows:
- **RFC 7519**: JSON Web Token (JWT) standard for audience claim
- **RFC 7515**: JSON Web Signature (JWS) for signature verification
- **OAuth 2.0 best practices**: Token scoping and validation patterns