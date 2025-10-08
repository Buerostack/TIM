# TIM 2.0 Data Flow Documentation

This document describes the key data flows within TIM 2.0 for different authentication and token management scenarios.

## OAuth2 Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant TIM as TIM API
    participant DB as Database
    participant Provider as OAuth2 Provider

    Client->>TIM: GET /oauth2/discovery
    TIM->>Client: Available providers list

    Client->>TIM: GET /oauth2/{provider}/login
    TIM->>TIM: Generate state & PKCE
    TIM->>DB: Store state & code_verifier
    TIM->>Client: Redirect to provider with state & code_challenge

    Client->>Provider: Authorization request
    Provider->>Provider: User authenticates
    Provider->>Client: Redirect with code & state

    Client->>TIM: GET /oauth2/callback?code=X&state=Y
    TIM->>DB: Validate state
    TIM->>Provider: Exchange code for token (with code_verifier)
    Provider->>TIM: Access token & ID token
    TIM->>TIM: Validate & parse tokens
    TIM->>DB: Store user session
    TIM->>Client: Set session cookie + redirect

    Client->>TIM: Authenticated requests (with cookie)
    TIM->>DB: Validate session
    TIM->>Client: Authorized response
```

## Custom JWT Generation Flow

```mermaid
sequenceDiagram
    participant Client
    participant TIM as TIM API
    participant KeyMgmt as Key Management
    participant DB as Database

    Client->>TIM: POST /jwt/custom/generate
    Note over Client,TIM: {JWTName, content, expirationInMinutes}

    TIM->>TIM: Validate request
    TIM->>KeyMgmt: Get private key
    KeyMgmt->>TIM: Private key

    TIM->>TIM: Generate JWT with claims
    Note over TIM: RS256 signature

    TIM->>DB: Store token metadata
    Note over DB: token_id, user, expiry, status

    TIM->>Client: JWT token response
    Note over Client: {token, expiresAt, tokenId}

    Client->>Client: Store token
    Client->>TIM: API request with Authorization: Bearer <token>
    TIM->>TIM: Validate signature
    TIM->>DB: Check revocation status
    TIM->>Client: Authorized response
```

## Token Validation Flow

```mermaid
sequenceDiagram
    participant Client
    participant TIM as TIM API
    participant KeyMgmt as Key Management
    participant DB as Database

    Client->>TIM: Request with Authorization: Bearer <token>

    TIM->>TIM: Extract JWT from header
    TIM->>TIM: Parse token (decode header & payload)

    alt Invalid format
        TIM->>Client: 401 Unauthorized
    end

    TIM->>KeyMgmt: Get public key (by kid)
    KeyMgmt->>TIM: Public key

    TIM->>TIM: Verify signature
    alt Signature invalid
        TIM->>Client: 401 Unauthorized
    end

    TIM->>TIM: Validate expiration
    alt Token expired
        TIM->>Client: 401 Unauthorized (Token expired)
    end

    TIM->>DB: Check revocation status
    DB->>TIM: Token status

    alt Token revoked
        TIM->>Client: 401 Unauthorized (Token revoked)
    end

    TIM->>TIM: Extract user claims
    TIM->>Client: 200 OK + Process request
```

## Token Lifecycle Management Flow

```mermaid
sequenceDiagram
    participant Client
    participant TIM as TIM API
    participant DB as Database

    Note over Client,DB: List Tokens
    Client->>TIM: POST /jwt/custom/list/me
    TIM->>TIM: Validate auth
    TIM->>DB: Query user's tokens
    DB->>TIM: Token list
    TIM->>Client: Filtered token list

    Note over Client,DB: Extend Token
    Client->>TIM: POST /jwt/custom/extend
    Note over Client,TIM: {tokenId, extensionInMinutes}
    TIM->>DB: Verify token ownership
    TIM->>DB: Update expiration
    TIM->>Client: Extended token details

    Note over Client,DB: Revoke Token
    Client->>TIM: POST /jwt/custom/revoke
    Note over Client,TIM: {tokenId}
    TIM->>DB: Verify token ownership
    TIM->>DB: Mark as revoked + audit log
    TIM->>Client: Revocation confirmation
```

## Public Key Distribution Flow

```mermaid
sequenceDiagram
    participant External as External Service
    participant TIM as TIM API
    participant KeyMgmt as Key Management

    External->>TIM: GET /jwt/keys/public
    TIM->>KeyMgmt: Get public keys
    KeyMgmt->>TIM: Public key set

    TIM->>TIM: Format as JWKS
    Note over TIM: JSON Web Key Set format

    TIM->>External: JWKS response
    Note over External: {keys: [{kty, kid, use, alg, n, e}]}

    External->>External: Cache keys
    External->>External: Validate TIM-issued tokens locally
```

## Session Management Flow

```mermaid
sequenceDiagram
    participant Client
    participant TIM as TIM API
    participant DB as Database
    participant Cache as Redis Cache

    Note over Client,Cache: Create Session
    Client->>TIM: OAuth2 callback (authenticated)
    TIM->>TIM: Generate session ID
    TIM->>DB: Store session data
    TIM->>Cache: Cache session (if Redis available)
    TIM->>Client: Set-Cookie: sessionId (HttpOnly, Secure)

    Note over Client,Cache: Validate Session
    Client->>TIM: Request with session cookie
    TIM->>Cache: Check session cache
    alt Cache hit
        Cache->>TIM: Session data
    else Cache miss
        TIM->>DB: Query session
        DB->>TIM: Session data
        TIM->>Cache: Update cache
    end

    TIM->>TIM: Validate expiration
    TIM->>Client: Authorized response

    Note over Client,Cache: Logout
    Client->>TIM: POST /oauth2/logout
    TIM->>DB: Delete session
    TIM->>Cache: Invalidate cache
    TIM->>Client: Clear-Cookie + redirect
```

## Data Storage Patterns

### Token Metadata Storage
- **token_id**: Unique identifier (UUID)
- **user_sub**: User subject claim
- **jwt_name**: Token name/label
- **issued_at**: Creation timestamp
- **expires_at**: Expiration timestamp
- **revoked**: Boolean status
- **revoked_at**: Revocation timestamp (if applicable)

### OAuth2 State Storage
- **state**: Random state parameter
- **provider**: Provider identifier
- **code_verifier**: PKCE code verifier
- **created_at**: State creation time
- **expires_at**: State expiration (short-lived)

### Session Storage
- **session_id**: Unique session identifier
- **user_sub**: Authenticated user
- **provider**: Authentication provider
- **created_at**: Session start
- **expires_at**: Session expiration
- **last_activity**: Last request timestamp
