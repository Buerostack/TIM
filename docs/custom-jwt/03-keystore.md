# Keystore in TIM

## What is `jwtkeystore.jks`?

`jwtkeystore.jks` is a Java KeyStore file where TIM stores its RSA private key and certificate.

- **Private key**: used by TIM to sign JWTs.  
- **Public key**: exposed via `/jwt/keys/public` so others can verify.

## Lifecycle

- On startup, TIM checks if `/opt/tim/jwtkeystore.jks` exists.  
- If not, it generates one automatically (dev/test default).  
- In production, you can mount a managed keystore instead.

## Configurable properties

- `jwt.signature.key-store` (path, e.g. `file:/opt/tim/jwtkeystore.jks`)  
- `jwt.signature.key-store-password`  
- `jwt.signature.key-store-type` (default: JKS)  
- `jwt.signature.key-alias` (default: jwtsign)  
