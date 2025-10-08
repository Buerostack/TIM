# Contributing to TIM 2.0

Thank you for your interest in contributing to TIM 2.0! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Documentation](#documentation)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

By participating in this project, you agree to:
- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

See the [Development Environment Setup Guide](docs/how-to/setup-development-environment.md#prerequisites) for detailed system requirements.

Quick checklist:
- Java 17+, Maven 3.8+, Docker, Git
- GitHub account for contributions

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/TIM.git
   cd TIM
   ```

3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/Buerostack/TIM.git
   ```

## Development Setup

Follow the [Development Environment Setup Guide](docs/how-to/setup-development-environment.md) for complete instructions.

Quick start: `docker-compose up -d postgres && mvn clean install && mvn spring-boot:run`

## Development Workflow

### 1. Create a Feature Branch

Always create a new branch for your work:

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
```

Branch naming conventions:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions or updates

### 2. Make Your Changes

- Write clean, maintainable code
- Follow the coding standards (see below)
- Add tests for new functionality
- Update documentation as needed

### 3. Test Your Changes

```bash
# Run all tests
mvn test

# Run specific module tests
cd app/custom-jwt
mvn test

# Run integration tests
mvn verify

# Check code coverage
mvn test jacoco:report
```

### 4. Commit Your Changes

Write clear, descriptive commit messages:

```bash
git add .
git commit -m "feat: Add token extension endpoint

- Implement POST /jwt/custom/extend endpoint
- Add ExtensionRequest DTO
- Update token expiration in database
- Add integration tests
"
```

Commit message format:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `test:` - Test additions or changes
- `refactor:` - Code refactoring
- `chore:` - Maintenance tasks

### 5. Push and Create Pull Request

```bash
# Push to your fork
git push origin feature/your-feature-name
```

Then create a pull request on GitHub.

## Coding Standards

### Java Code Style

- **Formatting**: Follow standard Java conventions
  - 4 spaces for indentation
  - Opening braces on same line
  - Maximum line length: 120 characters

- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Variables: `camelCase`

- **Package Structure**:
  ```
  com.buerostack.tim.{module}.{layer}
  Example: com.buerostack.tim.jwt.controller
  ```

### Code Organization

- **Controllers**: Handle HTTP requests, validate input, delegate to services
- **Services**: Contain business logic
- **Repositories**: Handle data access
- **DTOs**: Request/response data transfer objects
- **Models**: Domain entities

### Best Practices

1. **Single Responsibility**: Each class should have one clear purpose
2. **DRY (Don't Repeat Yourself)**: Extract common code into reusable methods
3. **Dependency Injection**: Use Spring's DI instead of creating objects manually
4. **Error Handling**: Use appropriate exceptions and return meaningful error messages
5. **Logging**: Use SLF4J for logging at appropriate levels
6. **Security**: Never log sensitive information (passwords, tokens, keys)

### Example: Controller

```java
@RestController
@RequestMapping("/jwt/custom")
@RequiredArgsConstructor
public class CustomJwtController {

    private final JwtService jwtService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateJwtResponse> generateToken(
            @Valid @RequestBody GenerateJwtRequest request) {

        try {
            JwtToken token = jwtService.generateToken(request);
            return ResponseEntity.ok(token.toResponse());
        } catch (InvalidClaimException e) {
            throw new BadRequestException("Invalid claims", e);
        }
    }
}
```

## Testing Requirements

All contributions must include tests with 80% minimum coverage.

**Requirements:**
- Unit tests for business logic
- Integration tests for endpoints
- 100% coverage for security-related code

**Quick Commands:**
```bash
mvn test                    # Run all tests
mvn test jacoco:report     # Generate coverage report
mvn test -Dtest=ClassName  # Run specific test
```

For detailed testing guidelines, see the [Testing Framework Documentation](testing/README.md).

## Pull Request Process

### Before Submitting

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Code coverage meets requirements (80%+)
- [ ] Documentation is updated
- [ ] Commit messages are clear and descriptive
- [ ] Code follows style guidelines
- [ ] No sensitive information (keys, passwords) in code

### PR Description Template

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Changes Made
- Bullet point list of changes

## Testing
- Describe how you tested these changes
- List any new test cases added

## Checklist
- [ ] Tests pass locally
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No breaking changes (or documented if necessary)
```

### Review Process

1. **Automated Checks**: CI/CD pipeline runs tests and checks
2. **Code Review**: Maintainers review code for quality and correctness
3. **Feedback**: Address reviewer comments
4. **Approval**: Once approved, PR will be merged

### After Merge

- Your contribution will be included in the next release
- You'll be credited in the release notes
- Thank you for contributing!

## Documentation

Documentation is as important as code.

**When adding features, update:**
- JavaDoc for all public methods
- User guides in `docs/how-to/` for new functionality
- OpenAPI specification for new endpoints
- Architecture docs for significant changes
- Add examples to `examples/` directory

**Documentation Standards:**
- Use Mermaid for diagrams
- Include code examples
- Write for clarity and brevity
- Cross-reference related documentation

## Reporting Issues

### Bug Reports

Include:
- **Title**: Clear, descriptive title
- **Description**: What happened vs. what you expected
- **Steps to Reproduce**: Detailed steps to reproduce the issue
- **Environment**: Java version, OS, TIM version
- **Logs**: Relevant error logs (sanitize sensitive data)
- **Screenshots**: If applicable

### Feature Requests

Include:
- **Title**: Clear description of the feature
- **Problem**: What problem does this solve?
- **Proposed Solution**: How should it work?
- **Alternatives**: Other solutions you considered
- **Context**: Any additional context

### Security Issues

**DO NOT** open public issues for security vulnerabilities.

Contact the maintainers privately:
- Email: security@buerostack.com (if available)
- Or create a private security advisory on GitHub

## Development Resources

**Useful Commands:**
```bash
mvn clean install              # Build
mvn spring-boot:run           # Run locally
mvn test jacoco:report        # Test with coverage
docker-compose logs -f tim    # View logs
```

**Project Structure:** See [docs/architecture/file-structure.md](docs/architecture/file-structure.md)

## Questions?

- **Documentation**: Check [docs/](docs/) directory
- **Examples**: See [examples/](examples/) for code samples
- **Issues**: Search existing issues on GitHub
- **Discussions**: Start a discussion on GitHub Discussions

## License

By contributing to TIM 2.0, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to TIM 2.0! Your efforts help make this project better for everyone.
