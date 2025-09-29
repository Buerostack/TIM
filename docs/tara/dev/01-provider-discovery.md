# User Story: OAuth2/OIDC Provider Discovery

## User Story
**As a** system administrator
**I want** TIM to automatically discover OAuth2/OIDC provider configuration
**So that** I can configure authentication providers without manual endpoint configuration

## Background
OAuth2/OIDC providers expose a discovery document at `/.well-known/openid-configuration` that contains all necessary endpoints, supported scopes, and capabilities. This eliminates manual configuration and ensures accuracy.

## Acceptance Criteria

### AC1: Automatic Discovery
**Given** a provider's discovery URL is configured
**When** TIM starts up or provider configuration is updated
**Then** TIM should fetch and parse the discovery document
**And** extract authorization endpoint, token endpoint, userinfo endpoint, and JWKS URI
**And** cache the configuration for subsequent use

### AC2: Discovery Document Validation
**Given** a discovery document is fetched
**When** parsing the document
**Then** TIM should validate required fields are present:
- `authorization_endpoint`
- `token_endpoint`
- `issuer`
- `jwks_uri`
- `response_types_supported` (must include "code")
- `grant_types_supported` (must include "authorization_code")

### AC3: Configuration Caching
**Given** a valid discovery document is parsed
**When** storing the configuration
**Then** TIM should cache the configuration with TTL (default 1 hour)
**And** automatically refresh expired configurations
**And** handle discovery endpoint failures gracefully

### AC4: Provider Validation
**Given** multiple providers are configured
**When** validating provider configurations
**Then** each provider must have unique identifier
**And** discovery URLs must be accessible and valid
**And** client credentials must be present

### AC5: Error Handling
**Given** a provider discovery fails
**When** the discovery document is unreachable or invalid
**Then** TIM should log the error with details
**And** mark the provider as unavailable
**And** continue operating with other valid providers
**And** retry discovery on a exponential backoff schedule

## Technical Requirements

### Discovery Document Fields
```json
{
  "issuer": "https://provider.example.com",
  "authorization_endpoint": "https://provider.example.com/auth",
  "token_endpoint": "https://provider.example.com/token",
  "userinfo_endpoint": "https://provider.example.com/userinfo",
  "jwks_uri": "https://provider.example.com/jwks",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code"],
  "scopes_supported": ["openid", "profile", "email"]
}
```

### Configuration Storage
```yaml
providers:
  tara:
    name: "TARA Estonia"
    discovery_url: "https://tara.ria.ee/oidc/.well-known/openid-configuration"
    client_id: "${TARA_CLIENT_ID}"
    client_secret: "${TARA_CLIENT_SECRET}"
    enabled: true
    cache_ttl: 3600  # seconds
```

## Implementation Notes
- Use HTTP client with appropriate timeouts (5-10 seconds)
- Implement exponential backoff for retries
- Support custom HTTP headers if needed (e.g., User-Agent)
- Log all discovery activities for debugging
- Consider provider-specific discovery URL patterns

## Definition of Done
- [ ] Discovery service fetches and parses OIDC discovery documents
- [ ] Configuration validation prevents invalid providers
- [ ] Caching mechanism with configurable TTL
- [ ] Error handling with retry logic
- [ ] Unit tests for all discovery scenarios
- [ ] Integration tests with real OIDC providers
- [ ] Documentation for configuration format