# JWT Basics

## What is JWT?

A JSON Web Token (JWT) is a compact, URL-safe means of representing claims between two parties.  
It has three parts:

```
<header>.<payload>.<signature>
```

- **Header**: algorithm & key info (e.g. RS256).  
- **Payload**: claims such as `sub`, `role`, `exp`.  
- **Signature**: cryptographic proof signed by TIM's private key.

## Verification model

- TIM uses its private key (from `jwtkeystore.jks`) to sign.  
- All consumers use TIM's public key (via `/jwt/keys/public`) to verify.  
- Only TIM can generate valid signatures; anyone can verify.  
