# How to Configure OAuth2 Providers

This guide explains how to configure OAuth2/OIDC identity providers in TIM 2.0.

## Overview

TIM 2.0 supports any OAuth2/OIDC compliant identity provider. This guide covers:
- Configuring built-in providers (Google, GitHub, TARA)
- Adding custom OIDC providers
- Testing provider configuration

## Configuration Methods

### Method 1: Environment Variable (Recommended)

Set the `OAUTH2_PROVIDERS` environment variable with JSON configuration:

```bash
export OAUTH2_PROVIDERS='{
  "google": {
    "clientId": "your-client-id.apps.googleusercontent.com",
    "clientSecret": "your-client-secret",
    "enabled": true
  }
}'
```

### Method 2: Application Configuration File

Edit `app/server/src/main/resources/application.yml`:

```yaml
oauth2:
  providers:
    google:
      clientId: your-client-id.apps.googleusercontent.com
      clientSecret: your-client-secret
      enabled: true
```

## Provider-Specific Configuration

### Google OAuth2

1. **Create OAuth2 Credentials**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing
   - Navigate to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth 2.0 Client ID"
   - Application type: "Web application"
   - Authorized redirect URIs: `http://localhost:8085/oauth2/callback`

2. **Configure TIM 2.0**:
```json
{
  "google": {
    "clientId": "123456789.apps.googleusercontent.com",
    "clientSecret": "GOCSPX-abc123def456",
    "enabled": true,
    "scope": "openid profile email"
  }
}
```

3. **Test**:
```bash
# Get authorization URL
curl http://localhost:8085/oauth2/discovery

# Navigate to login URL
curl http://localhost:8085/oauth2/google/login
```

### GitHub OAuth2

1. **Create OAuth App**:
   - Go to [GitHub Developer Settings](https://github.com/settings/developers)
   - Click "New OAuth App"
   - Application name: "TIM 2.0 Dev"
   - Homepage URL: `http://localhost:8085`
   - Authorization callback URL: `http://localhost:8085/oauth2/callback`

2. **Configure TIM 2.0**:
```json
{
  "github": {
    "clientId": "Iv1.a1b2c3d4e5f6g7h8",
    "clientSecret": "1234567890abcdef1234567890abcdef12345678",
    "enabled": true,
    "scope": "read:user user:email"
  }
}
```

### TARA Estonia

1. **Register Application**:
   - Contact RIA (Riigi Infosüsteemi Amet)
   - Obtain client credentials for test or production environment

2. **Configure TIM 2.0**:
```json
{
  "tara": {
    "clientId": "your-tara-client-id",
    "clientSecret": "your-tara-client-secret",
    "enabled": true,
    "issuerUrl": "https://tara-test.ria.ee",
    "scope": "openid"
  }
}
```

### Custom OIDC Provider

For any OpenID Connect compliant provider:

```json
{
  "custom-provider": {
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "enabled": true,
    "issuerUrl": "https://your-provider.com",
    "authorizationUri": "https://your-provider.com/oauth/authorize",
    "tokenUri": "https://your-provider.com/oauth/token",
    "userInfoUri": "https://your-provider.com/oauth/userinfo",
    "jwksUri": "https://your-provider.com/.well-known/jwks.json",
    "scope": "openid profile email"
  }
}
```

## Multi-Provider Configuration

Configure multiple providers simultaneously:

```json
{
  "google": {
    "clientId": "google-client-id",
    "clientSecret": "google-secret",
    "enabled": true
  },
  "github": {
    "clientId": "github-client-id",
    "clientSecret": "github-secret",
    "enabled": true
  },
  "microsoft": {
    "clientId": "microsoft-client-id",
    "clientSecret": "microsoft-secret",
    "enabled": true,
    "issuerUrl": "https://login.microsoftonline.com/common/v2.0"
  }
}
```

## Configuration Options Reference

### Core Options

| Option | Required | Description | Default |
|--------|----------|-------------|---------|
| `clientId` | Yes | OAuth2 client ID | - |
| `clientSecret` | Yes | OAuth2 client secret | - |
| `enabled` | No | Enable/disable provider | `true` |
| `scope` | No | OAuth2 scopes | `"openid profile email"` |

### OIDC Discovery Options

| Option | Required | Description |
|--------|----------|-------------|
| `issuerUrl` | No* | OIDC issuer URL for auto-discovery |
| `authorizationUri` | No* | Authorization endpoint URL |
| `tokenUri` | No* | Token endpoint URL |
| `userInfoUri` | No* | UserInfo endpoint URL |
| `jwksUri` | No* | JWKS endpoint URL |

*Either `issuerUrl` OR all individual URIs must be provided

## Testing Provider Configuration

### 1. Check Provider Discovery

```bash
curl http://localhost:8085/oauth2/discovery
```

Expected response:
```json
{
  "providers": [
    {
      "name": "google",
      "loginUrl": "/oauth2/google/login",
      "enabled": true
    },
    {
      "name": "github",
      "loginUrl": "/oauth2/github/login",
      "enabled": true
    }
  ]
}
```

### 2. Test Login Flow

```bash
# Get login URL
curl -L http://localhost:8085/oauth2/google/login

# This will redirect to Google's authorization page
# After authorization, you'll be redirected back to /oauth2/callback
```

### 3. Validate Session

After successful login, validate the session:

```bash
curl -X POST http://localhost:8085/oauth2/session/validate \
  -H "Cookie: JSESSIONID=your-session-id"
```

## Common Configuration Issues

### Issue: "Invalid redirect URI"

**Solution**: Ensure the redirect URI in TIM matches exactly what's configured in the provider:
```
TIM Callback: http://localhost:8085/oauth2/callback
Provider Setting: http://localhost:8085/oauth2/callback
```

### Issue: "Invalid scope"

**Solution**: Check provider documentation for supported scopes. Common scopes:
- Google: `openid profile email`
- GitHub: `read:user user:email`
- Microsoft: `openid profile email`

### Issue: "OIDC discovery failed"

**Solution**: Verify issuer URL and ensure it's accessible:
```bash
# Test OIDC discovery
curl https://accounts.google.com/.well-known/openid-configuration
```

## Production Considerations

### 1. Use Environment Variables

Never commit secrets to version control:

```bash
# .env (gitignored)
OAUTH2_PROVIDERS='{
  "google": {
    "clientId": "${GOOGLE_CLIENT_ID}",
    "clientSecret": "${GOOGLE_CLIENT_SECRET}",
    "enabled": true
  }
}'
```

### 2. Configure Redirect URIs

Update redirect URIs for production:

```json
{
  "google": {
    "clientId": "prod-client-id",
    "clientSecret": "prod-secret",
    "enabled": true,
    "redirectUri": "https://your-domain.com/oauth2/callback"
  }
}
```

### 3. Enable HTTPS

Always use HTTPS in production. Configure via `application.yml`:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 4. Rotate Secrets

Implement secret rotation:
- Rotate client secrets periodically
- Use secret management services (AWS Secrets Manager, HashiCorp Vault)
- Never use development credentials in production

## Advanced Configuration

### Custom User Claim Mapping

Map provider-specific claims to standard format:

```json
{
  "custom-provider": {
    "clientId": "client-id",
    "clientSecret": "secret",
    "enabled": true,
    "userMapping": {
      "sub": "user_id",
      "email": "email_address",
      "name": "full_name"
    }
  }
}
```

### PKCE Configuration

Enable PKCE for enhanced security:

```json
{
  "provider": {
    "clientId": "client-id",
    "clientSecret": "secret",
    "enabled": true,
    "pkceEnabled": true,
    "pkceMethod": "S256"
  }
}
```

## Next Steps

- **Authenticate Users**: Test OAuth2 login flows
- **Manage Sessions**: See session management documentation
- **Integrate with Applications**: Check `examples/oauth2-integration/`
