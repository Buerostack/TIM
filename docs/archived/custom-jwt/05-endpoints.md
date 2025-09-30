# TIM Endpoints

## Custom JWTs

- `POST /jwt/custom/generate`  
  Request body:
  ```json
  {
    "JWTName": "JWTTOKEN",
    "content": { "sub": "user-123", "role": "viewer" },
    "expirationInMinutes": 60,
    "setCookie": false
  }
  ```
  Response body:
  ```json
  {
    "status": "ok",
    "name": "JWTTOKEN",
    "token": "<the-actual-jwt>",
    "expiresAt": "2025-09-25T12:34:56Z"
  }
  ```

- `GET /jwt/keys/public`  
  Returns JWKS (JSON Web Key Set) for public verification.

## TARA

- `GET /tara/login` → Redirects to TARA.  
- `GET /tara/callback` → Exchanges TARA response, issues `TIM_TARA_JWT`.  
