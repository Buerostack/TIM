# TIM 2.0 File Structure

This document provides a comprehensive overview of the TIM 2.0 project structure, explaining the purpose and organization of each directory and key files.

## Project Root

```
TIM/
├── app/                        # Application source code
├── db/                         # Database initialization scripts
├── docs/                       # Documentation
├── examples/                   # Runnable code examples
├── .github/                    # GitHub configuration
├── docker-compose.yml          # Container orchestration
├── README.md                   # Project overview
├── LICENSE                     # MIT License
├── CHANGELOG.md                # Version history
├── CONTRIBUTING.md             # Contribution guidelines
└── pom.xml                     # Maven parent POM
```

## Application Module (`app/`)

The `app/` directory contains all application source code, organized as a multi-module Maven project.

```
app/
├── server/                     # Main Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/buerostack/tim/
│   │   │   │       ├── config/         # Spring configuration
│   │   │   │       ├── controller/     # REST controllers
│   │   │   │       ├── security/       # Security configuration
│   │   │   │       └── TimApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml     # Application configuration
│   │   │       └── application-prod.yml
│   │   └── test/
│   └── pom.xml
│
├── common/                     # Shared utilities and base classes
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/buerostack/tim/common/
│   │               ├── dto/            # Data Transfer Objects
│   │               ├── exception/      # Custom exceptions
│   │               └── util/           # Utility classes
│   └── pom.xml
│
├── custom-jwt/                 # Custom JWT management module
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/buerostack/tim/jwt/
│   │               ├── controller/     # JWT endpoints
│   │               ├── service/        # Business logic
│   │               ├── repository/     # Data access
│   │               ├── model/          # Domain entities
│   │               └── dto/            # Request/response DTOs
│   └── pom.xml
│
└── oauth2-oidc/                # OAuth2/OIDC authentication module
    ├── src/
    │   └── main/
    │       └── java/
    │           └── com/buerostack/tim/oauth2/
    │               ├── controller/     # OAuth2 endpoints
    │               ├── service/        # Provider integration
    │               ├── provider/       # Provider configurations
    │               ├── model/          # OAuth2 entities
    │               └── dto/            # OAuth2 DTOs
    └── pom.xml
```

## Database Module (`db/`)

Database initialization and migration scripts.

```
db/
├── init/                       # Initial schema creation
│   └── 01-schema.sql          # Database schema DDL
├── migrations/                 # Schema migrations (if using Flyway/Liquibase)
└── seed/                       # Sample data (development only)
```

## Documentation (`docs/`)

Comprehensive project documentation following ADR-001 requirements.

```
docs/
├── architecture/               # System architecture documentation
│   ├── overview.md            # Component overview with diagrams
│   ├── data-flow.md           # Data flow sequence diagrams
│   └── file-structure.md      # This file
│
├── how-to/                     # Step-by-step guides
│   ├── configure-oauth2-provider.md
│   ├── generate-custom-jwt.md
│   ├── setup-development-environment.md
│   └── deploy-production.md
│
├── reference/                  # Technical reference documentation
│   ├── openapi.yaml           # OpenAPI 3.1 specification
│   ├── api-endpoints.md       # Endpoint reference
│   ├── configuration.md       # Configuration options
│   └── database-schema.md     # Database schema reference
│
└── README.md                   # Documentation index
```

## Examples (`examples/`)

Runnable code examples demonstrating TIM 2.0 integration patterns.

```
examples/
├── basic-jwt-usage/            # Basic JWT generation example
│   ├── README.md              # What this example demonstrates
│   ├── index.js               # Runnable code
│   ├── package.json           # Dependencies
│   └── .env.example           # Configuration template
│
├── oauth2-integration/         # OAuth2 authentication example
│   ├── README.md
│   ├── server.js
│   ├── package.json
│   └── .env.example
│
└── token-validation/           # Token validation example
    ├── README.md
    ├── validator.js
    ├── package.json
    └── .env.example
```

## Configuration Files

### Root Level

- **`docker-compose.yml`**: Multi-container Docker configuration for development and production deployment
- **`pom.xml`**: Maven parent POM defining common dependencies and build configuration
- **`.gitignore`**: Git ignore patterns for Java, Maven, Docker, and IDE files
- **`.dockerignore`**: Docker build context exclusions

### Application Configuration

- **`app/server/src/main/resources/application.yml`**: Main application configuration
  - Database connection settings
  - Server port and context path
  - JWT key configuration
  - OAuth2 provider settings
  - Logging configuration

- **`app/server/src/main/resources/application-prod.yml`**: Production-specific overrides

## Key Source Files

### Main Application Entry Point
- `app/server/src/main/java/com/buerostack/tim/TimApplication.java`: Spring Boot application entry point

### Core Controllers
- `app/custom-jwt/src/main/java/com/buerostack/tim/jwt/controller/CustomJwtController.java`: Custom JWT management endpoints
- `app/oauth2-oidc/src/main/java/com/buerostack/tim/oauth2/controller/OAuth2Controller.java`: OAuth2 authentication endpoints

### Configuration Classes
- `app/server/src/main/java/com/buerostack/tim/config/SecurityConfig.java`: Spring Security configuration
- `app/server/src/main/java/com/buerostack/tim/config/OpenApiConfig.java`: OpenAPI/Swagger configuration
- `app/server/src/main/java/com/buerostack/tim/config/JwtConfig.java`: JWT key management configuration

## Build Artifacts

When building the project, the following artifacts are generated:

```
target/                         # Maven build output
├── tim-server.jar             # Executable Spring Boot JAR
├── classes/                   # Compiled .class files
└── generated-sources/         # Generated code (if any)
```

## Development Files

### IDE Configuration
- `.idea/`: IntelliJ IDEA project files (gitignored)
- `.vscode/`: VS Code workspace settings (gitignored)
- `*.iml`: IntelliJ module files (gitignored)

### Testing
- `app/*/src/test/`: Unit and integration tests for each module
- `test-reports/`: Test execution reports (gitignored)

## Environment-Specific Files

### Development
- `.env.local`: Local environment variables (gitignored)
- `docker-compose.override.yml`: Local Docker overrides (gitignored)

### Production
- Externalized configuration via environment variables
- Kubernetes manifests (if deploying to K8s)
- Cloud-specific deployment descriptors

## File Naming Conventions

### Java Classes
- **Controllers**: `*Controller.java` (e.g., `CustomJwtController.java`)
- **Services**: `*Service.java` (e.g., `TokenManagementService.java`)
- **Repositories**: `*Repository.java` (e.g., `JwtTokenRepository.java`)
- **DTOs**: `*Request.java`, `*Response.java` (e.g., `GenerateJwtRequest.java`)
- **Entities**: No suffix (e.g., `JwtToken.java`, `OAuth2Session.java`)
- **Exceptions**: `*Exception.java` (e.g., `TokenRevokedException.java`)

### Configuration Files
- YAML for application configuration: `application*.yml`
- Properties files: `*.properties`
- SQL scripts: `##-description.sql` (numbered for execution order)

### Documentation
- Markdown for all documentation: `*.md`
- OpenAPI specification: `openapi.yaml`
- Diagrams embedded in Markdown using Mermaid syntax

## Module Dependencies

```
server
  └── depends on: common, custom-jwt, oauth2-oidc

custom-jwt
  └── depends on: common

oauth2-oidc
  └── depends on: common

common
  └── standalone (no dependencies on other modules)
```

This modular structure ensures clear separation of concerns and enables independent development and testing of each functional area.
