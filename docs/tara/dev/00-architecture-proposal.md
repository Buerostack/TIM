# OAuth2/OIDC Generic Authentication Service - Architecture Proposal

## Overview
Design a **generic OAuth2/OpenID Connect authentication service** that supports multiple identity providers, including TARA (Estonia), but not limited to it. This approach ensures TIM can integrate with any standards-compliant OAuth2/OIDC provider.

## Core Architecture Principles

### 1. Provider-Agnostic Design
- **Generic OAuth2/OIDC client implementation**
- **Provider-specific configuration** (not hardcoded logic)
- **Standardized internal APIs** regardless of external provider
- **Plugin/adapter pattern** for provider-specific customizations

### 2. Standards Compliance
- **RFC 6749** (OAuth 2.0 Authorization Framework)
- **OpenID Connect Core 1.0** specification
- **RFC 7517** (JSON Web Key Set) for signature verification
- **Authorization Code Flow** as primary flow

### 3. Multi-Provider Support
```
TIM OAuth2 Service
├── Generic OIDC Client
├── Provider Configurations
│   ├── TARA (Estonia)
│   ├── Google OAuth2
│   ├── Azure AD
│   ├── Custom OIDC Provider
│   └── ... (extensible)
└── Unified Internal API
```

## Technical Components

### 1. Generic OIDC Client
- **Discovery**: Auto-configuration via `/.well-known/openid-configuration`
- **Authorization**: Standard authorization code flow
- **Token Exchange**: Access token + ID token handling
- **Token Validation**: Signature verification, expiration, issuer validation
- **UserInfo**: Profile data retrieval

### 2. Provider Configuration System
```yaml
providers:
  tara:
    name: "TARA Estonia"
    discovery_url: "https://tara.ria.ee/oidc/.well-known/openid-configuration"
    client_id: "${TARA_CLIENT_ID}"
    client_secret: "${TARA_CLIENT_SECRET}"
    scopes: ["openid", "idcard", "mid", "smartid"]
    custom_claims:
      - "authentication_method"
      - "level_of_assurance"

  google:
    name: "Google"
    discovery_url: "https://accounts.google.com/.well-known/openid-configuration"
    client_id: "${GOOGLE_CLIENT_ID}"
    client_secret: "${GOOGLE_CLIENT_SECRET}"
    scopes: ["openid", "profile", "email"]
```

### 3. Unified Internal API
Regardless of provider, TIM exposes consistent endpoints:
- `GET /oauth2/providers` - List available providers
- `GET /oauth2/auth/{provider}` - Initiate authentication
- `GET /oauth2/callback/{provider}` - Handle callback
- `POST /oauth2/token/validate` - Validate received tokens
- `GET /oauth2/profile` - Get user profile

## Benefits of Generic Approach

### 1. **Future-Proof**
- Easy to add new identity providers
- No vendor lock-in
- Adapts to provider changes via configuration

### 2. **Compliance & Standards**
- Follows OAuth2/OIDC best practices
- Interoperable with standard tooling
- Easier security auditing

### 3. **Operational Excellence**
- Single codebase for all providers
- Consistent error handling and logging
- Unified monitoring and metrics

### 4. **Developer Experience**
- Single API for all authentication flows
- Provider-agnostic client applications
- Clear separation of concerns

## TARA-Specific Features

While maintaining generic architecture, TARA-specific features can be handled via:

### 1. **Configuration-Based**
- Estonian-specific scopes (`idcard`, `mid`, `smartid`, `eidas`)
- Custom claim mappings (`authentication_method`, `level_of_assurance`)
- Estonia-specific validation rules

### 2. **Provider Adapters**
```java
public class TaraProviderAdapter implements OidcProviderAdapter {
    @Override
    public UserProfile mapUserProfile(IdToken idToken) {
        // TARA-specific claim mapping
        return UserProfile.builder()
            .personalCode(idToken.getClaim("personalcode"))
            .authenticationMethod(idToken.getClaim("amr"))
            .levelOfAssurance(idToken.getClaim("acr"))
            .build();
    }
}
```

### 3. **Custom Scopes & Claims**
- Handle TARA's authentication method scopes
- Map Estonia-specific identity attributes
- Support cross-border eIDAS claims

## Implementation Phases

### Phase 1: Generic OIDC Foundation
- Generic OAuth2/OIDC client implementation
- Provider configuration system
- Basic authentication flows

### Phase 2: TARA Integration
- TARA provider configuration
- Estonia-specific claim mappings
- Authentication method handling

### Phase 3: Multi-Provider Support
- Additional provider configurations
- Provider management APIs
- Advanced features (logout, refresh tokens)

## Security Considerations

### 1. **Standard Security Practices**
- PKCE (Proof Key for Code Exchange) support
- State parameter validation
- Nonce validation for ID tokens
- Token signature verification

### 2. **Provider Validation**
- Whitelist allowed providers
- Validate provider discovery documents
- Certificate pinning for critical providers

### 3. **Token Security**
- Short-lived access tokens
- Secure token storage
- Token revocation support

---

**Next Steps**: Create detailed user stories and acceptance criteria for each component.