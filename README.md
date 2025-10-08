# TIM 2.0 - Token Identity Manager

![Build Status](https://img.shields.io/badge/build-passing-brightgreen) ![Version](https://img.shields.io/badge/version-2.0-blue) ![License](https://img.shields.io/badge/license-MIT-green)

## About

TIM 2.0 (Token Identity Manager) is an advanced JWT and OAuth2 integration platform that provides comprehensive token lifecycle management and multi-provider authentication capabilities. It serves as a universal, provider-agnostic identity management solution for modern applications.

**Maintained by**: Rainer Türner
**Background**: TIM 2.0 evolved from the original TARA Integration Module (TIM) initially developed by the Information System Authority of Estonia (RIA). This repository represents a complete architectural rewrite, transforming TIM into a universal platform while maintaining backward compatibility with the original TARA-focused functionality.
**Status**: Active Development

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

For detailed documentation, see the [docs/](docs/) directory:

- **Architecture**: System design, component overview, and data flow diagrams
- **How-To Guides**: Step-by-step instructions for common tasks
- **API Reference**: Complete OpenAPI specification and endpoint documentation
- **Examples**: Runnable code examples for integration

**Interactive API Testing**: [Swagger UI](http://localhost:8085) - Live API testing interface

---

## Basic Usage

See the [examples/](examples/) directory for runnable code examples demonstrating:
- OAuth2 authentication flows
- Custom JWT token generation and validation
- Token lifecycle management
- Multi-provider integration

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute to this project, including:
- Development setup
- Code standards
- Testing requirements
- Pull request process

---

## License

TIM 2.0 is released under the MIT License. See [LICENSE](LICENSE) for complete license terms.

When using TIM 2.0, please include attribution:
```
TIM 2.0 - Token Identity Manager
Originally developed by RIA (Information System Authority of Estonia)
Original Architect: Rainer Türner
```