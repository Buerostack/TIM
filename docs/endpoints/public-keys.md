# Public Key Endpoints

## Overview
TIM 2.0 provides public key endpoints for JWT signature verification and cryptographic operations. These endpoints are typically used by external services to validate JWT tokens issued by TIM 2.0.

## Base URL
- Local Development: `http://localhost:8085`
- Production: Configure via environment variables

## Endpoints

### GET /jwt/keys/public
**Description**: Retrieve the public key(s) used for JWT signature verification
**Authentication**: None required (public endpoint)
**Response Format**: JSON Web Key Set (JWKS)
**Response**:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "jwtsign",
      "use": "sig",
      "alg": "RS256",
      "n": "base64-encoded-modulus...",
      "e": "AQAB"
    }
  ]
}
```

### Key Fields Explanation
- `kty`: Key type (always "RSA" for TIM)
- `kid`: Key ID identifier ("jwtsign")
- `use`: Key usage ("sig" for signature verification)
- `alg`: Algorithm ("RS256")
- `n`: RSA public key modulus (base64url encoded)
- `e`: RSA public key exponent (base64url encoded)

## Usage Scenarios

### JWT Verification
External services can use this endpoint to:
1. Fetch the current public key
2. Verify JWT signatures from TIM
3. Validate token authenticity

### Integration Example (Node.js)
```javascript
const jwt = require('jsonwebtoken');
const jwksClient = require('jwks-rsa');

const client = jwksClient({
  jwksUri: 'http://localhost:8085/jwt/keys/public'
});

function getKey(header, callback) {
  client.getSigningKey(header.kid, (err, key) => {
    const signingKey = key.publicKey || key.rsaPublicKey;
    callback(null, signingKey);
  });
}

// Verify a token
jwt.verify(token, getKey, {
  audience: 'tim-audience',
  issuer: 'TIM',
  algorithms: ['RS256']
}, (err, decoded) => {
  if (err) {
    console.error('Token verification failed:', err);
  } else {
    console.log('Token verified:', decoded);
  }
});
```

### Integration Example (Java)
```java
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

// Create JWK provider
JwkProvider provider = new UrlJwkProvider("http://localhost:8085/jwt/keys/public");

// Verify token
DecodedJWT jwt = JWT.decode(token);
RSAPublicKey publicKey = (RSAPublicKey) provider.get(jwt.getKeyId()).getPublicKey();
Algorithm algorithm = Algorithm.RSA256(publicKey, null);
algorithm.verify(jwt);
```

## Key Rotation
- TIM supports key rotation for enhanced security
- Multiple keys may be present during rotation periods
- Always use the `kid` field to identify the correct key
- Cached keys should be refreshed periodically

## Security Considerations
- Public keys are safe to cache and distribute
- Verify the `kid` matches the JWT header
- Implement proper error handling for key fetching
- Consider implementing key caching with TTL

## Performance Notes
- This endpoint is designed for high availability
- Caching is recommended (TTL: 1 hour)
- Keys change infrequently (only during rotation)
- No rate limiting applied to this endpoint

## Standards Compliance
- Follows RFC 7517 (JSON Web Key)
- Compatible with RFC 7515 (JSON Web Signature)
- JWKS format for easy integration with JWT libraries

## Error Handling
- **404 Not Found**: Key service not available
- **500 Internal Server Error**: Key generation failure
- Graceful degradation: Use cached keys when endpoint unavailable