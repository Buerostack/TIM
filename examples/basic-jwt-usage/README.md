# Basic JWT Usage Example

This example demonstrates the core functionality of TIM 2.0's custom JWT token management.

## What This Example Demonstrates

Complete JWT token lifecycle: generation, validation, listing, extension, and revocation.

## Prerequisites

- **TIM 2.0 running**: See [Development Setup Guide](../../docs/how-to/setup-development-environment.md)
- **Node.js 16+** and npm

## Installation

```bash
# Install dependencies
npm install
```

## Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` if your TIM instance is running on a different host/port:

```env
TIM_API_URL=http://localhost:8085
```

## Running the Example

### Full Demo

Run the complete demonstration:

```bash
npm start
```

This will:
1. Generate a new JWT token
2. Validate the token
3. List user's tokens
4. Extend token expiration
5. Make authenticated requests
6. Revoke the token

### Individual Operations

```bash
# Generate a token only
npm run generate

# Validate a token
npm run validate

# List tokens
npm run list

# Extend a token
npm run extend

# Revoke a token
npm run revoke
```

## Code Overview

This example demonstrates all JWT operations. See [index.js](index.js) for complete implementation.

**Key operations:**
1. Generate token with custom claims
2. Decode and validate token
3. List user's tokens
4. Extend token expiration
5. Make authenticated requests
6. Revoke token

For detailed JWT generation guide, see [docs/how-to/generate-custom-jwt.md](../../docs/how-to/generate-custom-jwt.md).

## Expected Output

```
🚀 TIM 2.0 - Basic JWT Usage Example
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Step 1: Generate JWT Token
   Token ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
   Expires: 2024-02-28T15:30:00Z
   Token: eyJraWQiOiJqd3RzaWduIi...

✅ Step 2: Decode Token (Client-Side)
   Header: { kid: 'jwtsign', alg: 'RS256' }
   Payload: {
     sub: 'user123',
     role: 'user',
     email: 'user@example.com',
     iss: 'TIM',
     exp: 1709131800
   }

✅ Step 3: List User Tokens
   Found 1 token(s)

✅ Step 4: Extend Token Expiration
   New expiration: 2024-02-28T16:00:00Z
   Extended by: 30 minutes

✅ Step 5: Make Authenticated Request
   Request successful!

✅ Step 6: Revoke Token
   Token revoked successfully
   Status: revoked

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✨ Example completed successfully!
```

## Error Handling

The example includes comprehensive error handling for common scenarios:

- **Network errors**: Connection to TIM API failed
- **Invalid tokens**: Expired or malformed tokens
- **Unauthorized**: Missing or invalid authentication
- **Not found**: Token ID doesn't exist
- **Already revoked**: Attempting to use a revoked token

## Integration Tips

**Security Best Practices:**
- Store tokens in HttpOnly cookies (use `setCookie: true`)
- Never store tokens in localStorage (XSS vulnerability)
- Use environment variables for service tokens
- Implement token refresh/extension for long sessions

For complete integration patterns, see [docs/how-to/generate-custom-jwt.md](../../docs/how-to/generate-custom-jwt.md).

## Testing

Run the test suite:

```bash
npm test
```

This will run integration tests against a live TIM instance.

## Troubleshooting

**Common Issues:**
- **Connection refused**: Ensure TIM is running (`docker-compose ps`)
- **Unauthorized errors**: Check token expiration and revocation status
- **Validation fails**: Verify system time synchronization

See [docs/how-to/setup-development-environment.md](../../docs/how-to/setup-development-environment.md#troubleshooting) for detailed troubleshooting.

## Next Steps

- [OAuth2 Provider Configuration](../../docs/how-to/configure-oauth2-provider.md)
- [JWT Generation Guide](../../docs/how-to/generate-custom-jwt.md)
- [Architecture Documentation](../../docs/architecture/overview.md)
