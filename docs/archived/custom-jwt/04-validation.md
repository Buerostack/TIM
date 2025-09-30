# Validating TIM-issued JWTs

## Why verification is distributed

- JWTs are signed once by TIM.  
- Any service can verify locally using TIM's public key.  
- TIM does not need to be called again for validation.  

## Steps

1. Receive `Authorization: Bearer <jwt>` header.  
2. Split token into `<header>.<payload>.<signature>`.  
3. Fetch public key from `/jwt/keys/public`.  
4. Verify signature using RS256.  

## Example (Node.js)

```js
const jwt = require("jsonwebtoken");
const fetch = require("node-fetch");

async function verifyToken(token) {
  const jwks = await fetch("http://localhost:8085/jwt/keys/public").then(r => r.json());
  const pubKey = jwks.keys[0]; // real code would convert modulus+exponent to PEM

  try {
    const decoded = jwt.verify(token, pubKey, { algorithms: ["RS256"] });
    console.log("Valid:", decoded);
  } catch (e) {
    console.error("Invalid:", e.message);
  }
}
```

## Example (Spring Boot)

```java
JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(timPublicKey).build();
Jwt jwt = decoder.decode(token);
```  
