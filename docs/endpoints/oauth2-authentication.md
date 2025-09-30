# OAuth2/OIDC Authentication Endpoints

## Overview
TIM provides OAuth2/OIDC authentication through multiple endpoints that handle provider discovery, login flows, token validation, and session management.

## Base URL
- Local Development: `http://localhost:8085`
- Production: Configure via environment variables

## Authentication Flow
1. **Discover Providers**: GET `/auth/providers` - List available OAuth2 providers
2. **Initiate Login**: GET `/auth/login/{providerId}` - Start OAuth2 flow
3. **Handle Callback**: GET `/auth/callback/{providerId}` - Process OAuth2 callback
4. **Validate Session**: GET `/auth/validate` - Check authentication status

## Endpoints

### GET /auth/health
**Description**: Health check for authentication service
**Parameters**: None
**Response**:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /auth/providers
**Description**: List all configured OAuth2/OIDC providers
**Parameters**: None
**Response**:
```json
{
  "providers": [
    {
      "id": "google",
      "name": "Google",
      "type": "oauth2"
    },
    {
      "id": "github",
      "name": "GitHub",
      "type": "oauth2"
    }
  ]
}
```

### GET /auth/login/{providerId}
**Description**: Initiate OAuth2 login flow with specified provider
**Parameters**:
- `providerId` (path): Provider identifier (e.g., "google", "github")
**Response**: HTTP 302 redirect to OAuth2 provider

### GET /auth/callback/{providerId}
**Description**: Handle OAuth2 callback after user authorization
**Parameters**:
- `providerId` (path): Provider identifier
- `code` (query): Authorization code from provider
- `state` (query): CSRF protection state parameter
**Response**: HTTP 302 redirect to application with session cookie

### GET /auth/validate
**Description**: Validate current authentication session
**Headers**:
- `Cookie`: Session cookie from authentication flow
**Response**:
```json
{
  "authenticated": true,
  "user": {
    "id": "user123",
    "email": "user@example.com",
    "provider": "google"
  },
  "expires": "2024-01-15T11:30:00Z"
}
```

### POST /auth/logout
**Description**: Terminate current authentication session
**Headers**:
- `Cookie`: Session cookie
**Response**:
```json
{
  "success": true,
  "message": "Successfully logged out"
}
```

## Security Considerations
- All endpoints use HTTPS in production
- CSRF protection via state parameter
- Secure session cookies with HttpOnly flag
- Provider-specific scopes configured per OAuth2 provider

## Error Handling
- **400 Bad Request**: Invalid provider ID or missing parameters
- **401 Unauthorized**: Invalid or expired session
- **404 Not Found**: Provider not configured
- **500 Internal Server Error**: Authentication service failure

## Integration Notes
- Session cookies are automatically managed by the browser
- Use `/auth/validate` before accessing protected resources
- Logout invalidates server-side session and clears cookies