# OAuth2/OIDC Testing Guide

## Overview
This guide covers testing the OAuth2/OIDC authentication system with various providers.

## Complete API Reference

### Provider Management Endpoints
```bash
# List all available providers
GET /auth/providers

# Get specific provider information
GET /auth/providers/{providerId}

# System health check
GET /auth/health
```

### Authentication Flow Endpoints
```bash
# Initiate OAuth2 authentication
GET /auth/login/{provider}

# Handle OAuth2 callback (automatic)
GET /auth/callback/{provider}
```

### Session Management Endpoints
```bash
# Validate session
GET /auth/session/validate?session_id={sessionId}

# Get user profile from session
GET /auth/profile?session_id={sessionId}

# Logout and revoke session
POST /auth/logout?session_id={sessionId}&reason={optional_reason}
```

## Quick Start Testing

### 1. Check Available Providers
```bash
curl -X GET http://localhost:8085/auth/providers
```

Expected response:
```json
{
  "providers": {
    "google": {
      "id": "google",
      "name": "Google OAuth2",
      "scopes": ["openid", "profile", "email"],
      "available": true
    },
    "tara": {
      "id": "tara",
      "name": "TARA Estonia",
      "scopes": ["openid", "idcard", "mid", "smartid"],
      "available": true
    }
  },
  "total": 2
}
```

### 2. Test Provider Information
```bash
curl -X GET http://localhost:8085/auth/providers/google
```

### 3. Initiate Authentication Flow
```bash
curl -X GET http://localhost:8085/auth/login/google
```

Expected response:
```json
{
  "authorization_url": "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=...",
  "provider": "google",
  "state": "random_state_value"
}
```

## Google OAuth2 Testing

### Prerequisites
1. Create Google OAuth2 credentials:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing
   - Enable Google+ API
   - Create OAuth 2.0 credentials
   - Add redirect URI: `http://localhost:8085/auth/callback/google`

2. Set environment variables:
```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

### Testing Steps

1. **Start Authentication**:
```bash
curl -X GET http://localhost:8085/auth/login/google
```

2. **Copy the authorization_url** from response and open in browser

3. **Complete Google authentication** - you'll be redirected to:
```
http://localhost:8085/auth/callback/google?code=4/...&state=...
```

4. **Check the response** - should contain:
```json
{
  "status": "authentication_success",
  "provider": "google",
  "session_id": "sess_AbCdEf123456...",
  "expires_at": "2025-09-30T17:00:00Z",
  "user_profile": {
    "sub": "google-user-id",
    "provider": "google",
    "profile": {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@gmail.com",
      "emailVerified": true,
      "avatarUrl": "https://..."
    },
    "token_info": {
      "issued_at": "2025-09-29T17:00:00Z",
      "expires_at": "2025-09-29T18:00:00Z",
      "scopes": ["openid", "profile", "email"]
    }
  },
  "message": "Authentication completed successfully"
}
```

### 5. Test Session Management
```bash
# Validate the session
curl "http://localhost:8085/auth/session/validate?session_id=sess_AbCdEf123456..."

# Get user profile
curl "http://localhost:8085/auth/profile?session_id=sess_AbCdEf123456..."

# Logout
curl -X POST "http://localhost:8085/auth/logout?session_id=sess_AbCdEf123456..."
```

## TARA Testing

### Prerequisites
1. Register with RIA for TARA test credentials:
   - Contact: ria@ria.ee
   - Request test environment access
   - Provide redirect URI: `http://localhost:8085/auth/callback/tara`

2. Set environment variables:
```bash
export TARA_CLIENT_ID="your-tara-client-id"
export TARA_CLIENT_SECRET="your-tara-client-secret"
```

### Testing Steps

1. **Start TARA Authentication**:
```bash
curl -X GET http://localhost:8085/auth/login/tara
```

2. **Use TARA test environment** - authentication URL will be:
```
https://tara-test.ria.ee/oidc/authorize?...
```

3. **Test with TARA Demo User**:
   - Use Estonian test ID card
   - Or Mobile-ID test number: +37200000766
   - PIN: 1234

## Error Testing

### Invalid Provider
```bash
curl -X GET http://localhost:8085/auth/login/invalid-provider
```

Expected response:
```json
{
  "error": "provider_not_available",
  "message": "Provider not found or disabled: invalid-provider",
  "available_providers": ["google", "tara"]
}
```

### Invalid Callback
```bash
curl -X GET "http://localhost:8085/auth/callback/google?error=access_denied&error_description=User+denied+access"
```

Expected response:
```json
{
  "error": "access_denied",
  "message": "User denied access",
  "provider": "google"
}
```

## Configuration Testing

### Test Discovery Endpoints
```bash
# Google
curl -s https://accounts.google.com/.well-known/openid-configuration | jq '.'

# TARA Test
curl -s https://tara-test.ria.ee/oidc/.well-known/openid-configuration | jq '.'
```

### Test JWKS Endpoints
```bash
# Google
curl -s https://www.googleapis.com/oauth2/v3/certs | jq '.'

# TARA Test
curl -s https://tara-test.ria.ee/oidc/jwks | jq '.'
```

## Health Check
```bash
curl -X GET http://localhost:8085/auth/health
```

Expected response:
```json
{
  "status": "healthy",
  "service": "oauth2-authentication",
  "timestamp": 1727628000000,
  "available_providers": 2,
  "provider_ids": ["google", "tara"]
}
```

## Troubleshooting

### Common Issues

1. **Provider not available**
   - Check configuration in `oauth2-providers.yml`
   - Verify environment variables are set
   - Check discovery endpoint accessibility

2. **Token exchange failed**
   - Verify client credentials
   - Check redirect URI matches configuration
   - Ensure callback URL is accessible

3. **JWT validation failed**
   - Check system clock synchronization
   - Verify issuer and audience claims
   - Check key rotation

### Debug Logging
Enable debug logging by setting:
```
logging.level.buerostack.oauth2=DEBUG
```

### Network Testing
Test connectivity to providers:
```bash
# Google
curl -I https://accounts.google.com/.well-known/openid-configuration

# TARA
curl -I https://tara-test.ria.ee/oidc/.well-known/openid-configuration
```

## Security Testing

### Test State Parameter Validation
1. Initiate auth flow and capture state parameter
2. Try reusing the same state parameter - should fail
3. Try using invalid state parameter - should fail

### Test Nonce Validation
1. Check that nonce in ID token matches request nonce
2. Verify nonce prevents replay attacks

### Test Token Expiration
1. Wait for token expiration
2. Try using expired token - should fail validation