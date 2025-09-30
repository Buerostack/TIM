# TIM 2.0 - Token Identity Manager
Advanced JWT & OAuth2 Integration Platform

## Origin

TIM 2.0 evolved from the original TARA Integration Module (TIM) that was initially developed by the Information System Authority of Estonia (RIA) under the leadership of [Rainer Türner](https://www.linkedin.com/in/rainer-t%C3%BCrner-058b80b8/). The original TIM was designed specifically for integration with Estonia's national e-authentication service TARA.

This repository represents a complete architectural rewrite, transforming TIM into a universal, provider-agnostic JWT and OAuth2 integration platform while maintaining backward compatibility with the original TARA-focused functionality.

**Original Architect & Current Tech Lead**: [Rainer Türner](https://www.linkedin.com/in/rainer-t%C3%BCrner-058b80b8/)
**Code Generation**: Assisted by Claude Code (Anthropic)

## Current Repository

TIM 2.0 is maintained as an independent project, expanding beyond its Estonian government origins to serve as a comprehensive token identity management solution for organizations worldwide. The platform maintains its core security principles while adding enterprise-grade features and multi-provider support.

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)

### Running with Docker
```bash
git clone https://github.com/Buerostack/TIM.git
cd TIM
docker-compose up -d
```

### Access the API
- **Swagger UI**: http://localhost:8085
- **Health Check**: http://localhost:8085/auth/health
- **Public Keys (JWKS)**: http://localhost:8085/jwt/keys/public

### Generate Your First Token
```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "test-token",
    "content": {
      "sub": "user123",
      "role": "admin"
    },
    "expirationInMinutes": 60
  }'
```

### Test Authentication
```bash
# Use the token from previous step
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json"
```

---

## Core Features

### 🔐 Authentication Methods
- **OAuth2/OIDC Providers**: Google, GitHub, TARA Estonia, and custom providers
- **Custom JWT Tokens**: Self-managed tokens with configurable claims and expiration
- **Session Management**: Secure cookie-based sessions for web applications

### 🎫 Token Lifecycle Management
- **Generation**: Create tokens with custom claims, expiration, and metadata
- **Listing**: View and filter tokens by user, status, and date ranges
- **Extension**: Extend token expiration for active sessions
- **Revocation**: Immediate token invalidation with audit trails
- **Validation**: Real-time signature and status verification

### 🏢 Enterprise Features
- **Provider Agnostic**: Configure any OAuth2/OIDC compliant identity provider
- **Multi-tenant Ready**: Isolated token spaces and configurations
- **Audit Logging**: Comprehensive token lifecycle tracking
- **High Performance**: Optimized for large-scale deployments
- **Security First**: RSA256 signatures, PKCE support, CSRF protection

---

## Documentation

### 📚 API Documentation
- **[Complete API Guide](docs/README.md)** - Comprehensive documentation with examples
- **[Interactive Swagger UI](http://localhost:8085)** - Live API testing interface

### 🔗 Endpoint References
- **[OAuth2 Authentication](docs/endpoints/oauth2-authentication.md)** - Provider discovery, login flows, session validation
- **[JWT Management](docs/endpoints/custom-jwt-management.md)** - Token generation, listing, extension, revocation
- **[Public Keys](docs/endpoints/public-keys.md)** - JWKS endpoints for signature verification

### 🗄️ Technical Details
- **[Database Schema](docs/database/schema.md)** - Complete schema documentation with indexes
- **[OpenAPI Specification](docs/api/openapi.yaml)** - Machine-readable API specification

---

## Development

### Local Development Setup
```bash
# Clone repository
git clone https://github.com/Buerostack/TIM.git
cd TIM

# Start dependencies (PostgreSQL)
docker-compose up -d postgres

# Run application
cd app
mvn spring-boot:run
```

### Building from Source
```bash
# Build all modules
mvn clean package

# Build Docker image
docker-compose build tim

# Run tests
mvn test
```

### Project Structure
```
TIM/
├── app/                    # Application modules
│   ├── server/             # Main Spring Boot application
│   ├── common/             # Shared utilities
│   ├── custom-jwt/         # JWT management module
│   └── oauth2-oidc/        # OAuth2 authentication module
├── db/                     # Database initialization
├── docs/                   # API and technical documentation
└── docker-compose.yml     # Container orchestration
```

---

## Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/tim
DATABASE_USERNAME=tim
DATABASE_PASSWORD=tim

# JWT Signing
KEY_PASS=changeme

# OAuth2 Providers (JSON configuration)
OAUTH2_PROVIDERS={"google":{"clientId":"...","clientSecret":"..."}}
```

### Provider Configuration
TIM 2.0 supports any OAuth2/OIDC compliant provider. See documentation for configuration examples with:
- Google OAuth2
- GitHub OAuth2
- Microsoft Azure AD
- TARA Estonia
- Custom OIDC providers

---

## Security

### Key Features
- **RSA256 JWT Signatures**: Industry-standard token signing
- **Token Revocation**: Immediate invalidation capability
- **PKCE Support**: Enhanced OAuth2 security
- **CSRF Protection**: State parameter validation
- **Secure Sessions**: HttpOnly cookies with proper expiration

### Best Practices
- Rotate JWT signing keys regularly
- Use appropriate token expiration times
- Implement proper error handling
- Monitor token usage patterns
- Keep dependencies updated

---

## License

TIM 2.0 is released under the MIT License, allowing free use by anyone while requiring attribution to the original source.

### Attribution Requirements
When using TIM 2.0, please include the following attribution:

```
TIM 2.0 - Token Identity Manager
Originally developed by RIA (Information System Authority of Estonia)
Original Architect: Rainer Türner
Current Maintainer: Rainer Türner
Code Generation Assistance: Claude Code (Anthropic)
```

See [LICENSE](LICENSE) file for complete license terms.

---

## Support & Contributing

### Documentation
- **API Documentation**: Complete guides available in [docs/](docs/)
- **Interactive Testing**: Swagger UI at http://localhost:8085
- **Examples**: Integration examples for common use cases

### Getting Help
- Review the comprehensive [API documentation](docs/README.md)
- Check the [database schema guide](docs/database/schema.md)
- Test endpoints using the [interactive Swagger UI](http://localhost:8085)

### Reporting Issues
When reporting issues, please include:
- TIM 2.0 version
- Environment details (Docker, Java version)
- Error logs and reproduction steps
- Expected vs actual behavior

---

*TIM 2.0 maintains the security principles and reliability of the original RIA-developed TARA Integration Module while expanding capabilities for global enterprise use.*