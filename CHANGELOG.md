# Changelog

All notable changes to TIM 2.0 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive documentation structure following ADR-001 software publishing rules
- Architecture documentation with Mermaid diagrams
  - Component overview diagram
  - Data flow sequence diagrams
  - File structure documentation
- How-to guides
  - Development environment setup
  - Custom JWT token generation
  - OAuth2 provider configuration
- API reference documentation with OpenAPI 3.0 specification
- Runnable examples
  - Basic JWT usage example with Node.js

### Changed
- Updated README.md to comply with ADR-002 structure requirements
  - Added About section with maintainer and status information
  - Reorganized sections for better clarity
  - Simplified documentation references

## [2.0.0] - 2025-01-XX

### Added
- Complete architectural rewrite from original TARA Integration Module
- Universal OAuth2/OIDC provider support
- Custom JWT token generation and management
- Comprehensive token lifecycle management
  - Token generation with custom claims
  - Token listing and filtering
  - Token expiration extension
  - Token revocation with audit trails
- RFC 7662 compliant token introspection endpoint
- Multi-provider authentication support
  - Google OAuth2
  - GitHub OAuth2
  - TARA Estonia
  - Custom OIDC providers
- Enterprise features
  - RSA256 JWT signatures
  - PKCE support for OAuth2
  - CSRF protection with state parameters
  - Session management with secure cookies
  - Audit logging for token operations
- Public JWKS endpoint for external token validation
- Docker Compose orchestration for easy deployment
- Comprehensive OpenAPI 3.0 specification
- Interactive Swagger UI documentation
- Health check endpoints

### Changed
- Migrated from TARA-specific implementation to provider-agnostic design
- Updated authentication flows to support multiple providers
- Improved security with modern cryptographic standards
- Enhanced database schema for token metadata tracking
- Modernized API design following REST best practices

### Security
- Implemented RSA256 token signing
- Added token revocation capability
- Enabled PKCE for OAuth2 flows
- Implemented CSRF protection
- Configured secure session management with HttpOnly cookies

## [1.x.x] - Historical

Previous versions were maintained as the TARA Integration Module (TIM) by the Information System Authority of Estonia (RIA), focusing specifically on TARA authentication integration.

### Historical Context
- Original architect: Rainer Türner
- Original organization: RIA (Information System Authority of Estonia)
- Original purpose: TARA Estonia authentication integration
- License: MIT (maintained in version 2.0)

---

## Migration Guide

### From TIM 1.x to TIM 2.0

TIM 2.0 maintains backward compatibility with TARA Estonia authentication while adding support for multiple OAuth2/OIDC providers.

#### Configuration Changes

**TIM 1.x** (TARA-only):
```yaml
tara:
  clientId: xxx
  clientSecret: yyy
```

**TIM 2.0** (Multi-provider):
```yaml
oauth2:
  providers:
    tara:
      clientId: xxx
      clientSecret: yyy
      enabled: true
```

#### API Endpoint Changes

| TIM 1.x | TIM 2.0 | Notes |
|---------|---------|-------|
| `/tara/login` | `/oauth2/tara/login` | TARA-specific endpoint |
| N/A | `/oauth2/{provider}/login` | New: Support for any provider |
| `/tara/callback` | `/oauth2/callback` | Unified callback for all providers |
| N/A | `/jwt/custom/generate` | New: Custom JWT generation |
| N/A | `/jwt/custom/list/me` | New: Token management |

#### Database Schema

TIM 2.0 introduces new tables for token management. Migration scripts are provided in `db/migrations/`.

---

## Upgrade Instructions

### Upgrading to 2.0.0

1. **Backup your database**:
   ```bash
   docker-compose exec postgres pg_dump -U tim tim > backup.sql
   ```

2. **Update configuration**:
   - Review `application.yml` changes
   - Update OAuth2 provider configuration
   - Set new environment variables

3. **Run database migrations**:
   ```bash
   docker-compose up -d postgres
   # Migrations run automatically on startup
   ```

4. **Update client applications**:
   - Update API endpoints to new paths
   - Add Authorization headers for JWT tokens
   - Update callback URLs for OAuth2

5. **Test thoroughly**:
   - Verify TARA authentication (if used)
   - Test new custom JWT features
   - Validate token lifecycle operations

---

## Deprecation Notices

### Deprecated in 2.0.0
- **Direct TARA endpoints**: Use `/oauth2/tara/*` instead of `/tara/*`
- **Legacy session format**: Migrated to new session structure

### Removed in 2.0.0
- **Hardcoded TARA configuration**: Now uses dynamic provider configuration

---

## Contributors

TIM 2.0 represents the collaborative effort of:
- **Original Architect**: Rainer Türner (RIA Estonia)
- **Current Development**: Rainer Türner
- **Code Generation Assistance**: Claude Code (Anthropic)

---

## License

TIM 2.0 is released under the MIT License, maintaining the open-source nature of the original TARA Integration Module.

See [LICENSE](LICENSE) for complete license terms.
