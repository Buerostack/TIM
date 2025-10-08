# Basic JWT Usage Example

This example demonstrates the core functionality of TIM 2.0's custom JWT token management.

## What This Example Demonstrates

- Generating custom JWT tokens with claims
- Validating and decoding JWT tokens
- Using tokens for authenticated requests
- Listing and managing tokens
- Extending token expiration
- Revoking tokens

## Prerequisites

- TIM 2.0 running on `http://localhost:8085` (see [setup guide](../../docs/how-to/setup-development-environment.md))
- Node.js 16+ installed
- npm or yarn package manager

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

### 1. Generate Token

```javascript
const response = await axios.post(`${TIM_API_URL}/jwt/custom/generate`, {
  JWTName: 'example-token',
  content: {
    sub: 'user123',
    role: 'user',
    email: 'user@example.com'
  },
  expirationInMinutes: 60
});

const { token, expiresAt, tokenId } = response.data;
```

### 2. Use Token for Authenticated Requests

```javascript
const response = await axios.post(
  `${TIM_API_URL}/jwt/custom/list/me`,
  {},
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

### 3. Decode and Validate Token

```javascript
// Decode token (without verification)
const decoded = jwt.decode(token, { complete: true });

// Validate token with TIM
const validationResponse = await axios.post(
  `${TIM_API_URL}/jwt/validate`,
  { token }
);
```

### 4. Extend Token Expiration

```javascript
const response = await axios.post(
  `${TIM_API_URL}/jwt/custom/extend`,
  {
    tokenId: tokenId,
    extensionInMinutes: 30
  },
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

### 5. Revoke Token

```javascript
const response = await axios.post(
  `${TIM_API_URL}/jwt/custom/revoke`,
  {
    tokenId: tokenId
  },
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

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

### Web Applications

Store tokens securely:
```javascript
// ❌ DON'T: Store in localStorage (vulnerable to XSS)
localStorage.setItem('token', token);

// ✅ DO: Use HttpOnly cookies (set by TIM with setCookie: true)
// Or store in memory only for SPA
let authToken = null;
```

### API Services

Use environment variables for service tokens:
```javascript
const SERVICE_TOKEN = process.env.TIM_SERVICE_TOKEN;

const response = await axios.get(url, {
  headers: {
    'Authorization': `Bearer ${SERVICE_TOKEN}`
  }
});
```

### Token Refresh Pattern

```javascript
async function makeAuthenticatedRequest(url) {
  try {
    return await axios.get(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
  } catch (error) {
    if (error.response?.status === 401) {
      // Token expired, extend or generate new
      await extendToken();
      // Retry request
      return await axios.get(url, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
    }
    throw error;
  }
}
```

## Testing

Run the test suite:

```bash
npm test
```

This will run integration tests against a live TIM instance.

## Troubleshooting

### "Connection refused" error
- Ensure TIM 2.0 is running: `docker-compose ps`
- Check TIM URL in `.env` matches your setup

### "Unauthorized" errors
- Verify token is not expired
- Check token is not revoked
- Ensure token is included in Authorization header

### Token validation fails
- Check system time synchronization (for exp/iat claims)
- Verify TIM's public keys are accessible

## Next Steps

- **OAuth2 Integration**: See [../oauth2-integration/](../oauth2-integration/)
- **Token Validation**: See [../token-validation/](../token-validation/)
- **Production Deployment**: See [docs/how-to/deploy-production.md](../../docs/how-to/deploy-production.md)

## License

MIT License - See [LICENSE](../../LICENSE)
