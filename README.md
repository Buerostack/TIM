# TIM 2.0 - Token Identity Manager

![Build Status](https://img.shields.io/badge/build-passing-brightgreen) ![Version](https://img.shields.io/badge/version-2.0-blue) ![License](https://img.shields.io/badge/license-MIT-green)

## About

TIM 2.0 (Token Identity Manager) is an advanced JWT and OAuth2 integration platform that provides comprehensive token lifecycle management and multi-provider authentication capabilities. It serves as a universal, provider-agnostic identity management solution for modern applications.

**Maintained by**: Rainer Türner
**Status**: Active Development

## Origin

Originally developed at **Information System Authority of Estonia** (Riigi Infosüsteemi Amet, RIA).

- **Original Repository**: [https://github.com/buerokratt/TIM](https://github.com/buerokratt/TIM)
- **Initial Development**: As TARA Integration Module for Estonia's e-authentication service
- **Original Lead Developer**: [Rainer Türner](https://www.linkedin.com/in/rainer-t%C3%BCrner-058b80b8/)

This cleaned repository, started on **October 8, 2025**, is maintained by Rainer Türner to continue development of TIM as a universal, provider-agnostic platform while preserving the core security principles of the original implementation. The repository represents a complete architectural rewrite, expanding from TARA-specific integration to comprehensive multi-provider OAuth2/OIDC and custom JWT management capabilities.


## Quick Start

```bash
git clone https://github.com/Buerostack/TIM.git
cd TIM
docker-compose up -d
```

Access TIM at **http://localhost:8085**

For detailed setup instructions, see the [Development Environment Setup Guide](docs/how-to/setup-development-environment.md).

---

## Installation

### Prerequisites
- Docker and Docker Compose
- Git

### Setup
```bash
git clone https://github.com/Buerostack/TIM.git
cd TIM
docker-compose up -d
```

The application will be available at **http://localhost:8085**

For detailed setup instructions, see the [Development Environment Setup Guide](docs/how-to/setup-development-environment.md).

---

## Basic Usage

### Core Features
- **OAuth2/OIDC Authentication**: Multi-provider support (Google, GitHub, TARA, custom)
- **Custom JWT Management**: Generate, extend, revoke, and validate tokens
- **Token Lifecycle Tracking**: Complete audit trails with extension chains
- **Enterprise Security**: RSA256 signatures, PKCE, CSRF protection, token revocation
- **Developer Friendly**: OpenAPI docs, runnable examples, comprehensive guides

### Generate a Token

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{"JWTName": "my-token", "content": {"sub": "user123"}, "expirationInMinutes": 60}'
```

### Detailed Guides
- [Generate Custom JWT Tokens](docs/how-to/generate-custom-jwt.md)
- [Configure OAuth2 Providers](docs/how-to/configure-oauth2-provider.md)
- [Runnable Examples](examples/) - Complete integration examples

---

## Documentation

For detailed documentation, see the [docs/](docs/) directory:

- **Architecture**: System design, component overview, and data flow diagrams
- **How-To Guides**: Step-by-step instructions for common tasks
- **API Reference**: Complete OpenAPI specification and endpoint documentation
- **Examples**: Runnable code examples for integration

**Interactive API Testing**: [Swagger UI](http://localhost:8085) - Live API testing interface

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