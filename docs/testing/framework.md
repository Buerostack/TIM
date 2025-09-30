# TIM 2.0 Testing Framework

## Overview

TIM 2.0 includes a comprehensive testing framework designed to validate the new JWT extension chain functionality and ensure database schema integrity. The testing framework covers unit tests, integration tests, and database validation tests.

## Test Structure

```
app/
├── custom-jwt/src/test/java/buerostack/jwt/
│   ├── entity/CustomJwtMetadataTest.java          # Entity unit tests
│   ├── repo/CustomJwtMetadataRepoTest.java        # Repository tests
│   └── service/CustomJwtServiceTest.java          # Service layer tests
└── server/src/test/java/buerostack/
    ├── introspection/TokenIntrospectionIntegrationTest.java  # API integration tests
    └── database/SchemaValidationTest.java         # Database schema validation
```

## Test Categories

### 1. Entity Unit Tests (`CustomJwtMetadataTest`)

**Purpose**: Validate the JWT metadata entity with new extension chain fields.

**Key Test Areas**:
- Default constructor behavior (ID and timestamp generation)
- Parameterized constructor validation
- Extension chain relationship setup
- Immutable field behavior
- Getter/setter functionality

**Extension Chain Scenarios**:
```java
// New token scenario
CustomJwtMetadata newToken = new CustomJwtMetadata(
    jwtUuid,                    // jwt_uuid
    claimKeys,
    issuedAt,
    expiresAt,
    jwtUuid                     // original_jwt_uuid = jwt_uuid for new tokens
);

// Extension scenario
CustomJwtMetadata extension = new CustomJwtMetadata(
    newJwtUuid,                 // New JWT UUID
    claimKeys,
    newIssuedAt,
    extendedExpiresAt,
    originalJwtUuid             // Points to original
);
extension.setSupersedes(previousVersionId);
```

### 2. Repository Tests (`CustomJwtMetadataRepoTest`)

**Purpose**: Validate database queries for extension chain functionality.

**Test Methods**:
- `testFindCurrentVersionByJwtUuid()` - Get latest version of a JWT
- `testFindAllVersionsByOriginalJwtUuid()` - Get complete extension chain
- `testFindActiveJwtsBySubject()` - Get current versions for user
- `testExtensionChainIntegrity()` - Validate chain relationships

**Database Setup**:
```java
@BeforeEach
void setUp() {
    // Create original token
    originalToken = new CustomJwtMetadata(originalJwtUuid, ..., originalJwtUuid);

    // Create first extension
    extension1 = new CustomJwtMetadata(extension1JwtUuid, ..., originalJwtUuid);
    extension1.setSupersedes(originalToken.getId());

    // Create second extension
    extension2 = new CustomJwtMetadata(extension2JwtUuid, ..., originalJwtUuid);
    extension2.setSupersedes(extension1.getId());
}
```

### 3. Service Tests (`CustomJwtServiceTest`)

**Purpose**: Validate JWT generation and extension business logic.

**Mock Configuration**:
- `JwtSignerService` - JWT creation and verification
- `CustomDenylistRepo` - Token revocation checking
- `CustomJwtMetadataRepo` - Metadata persistence

**Key Test Scenarios**:
- JWT generation with metadata creation
- JWT extension with chain tracking
- Rejection of expired/revoked tokens
- Proper denylist management

**Extension Chain Validation**:
```java
verify(metadataRepo).save(argThat(metadata -> {
    assertEquals(originalJwtId, metadata.getOriginalJwtUuid());
    assertEquals(previousMetadataId, metadata.getSupersedes());
    assertNotEquals(originalJwtId, metadata.getJwtUuid());
    return true;
}));
```

### 4. Integration Tests (`TokenIntrospectionIntegrationTest`)

**Purpose**: End-to-end validation of token introspection with extension chain support.

**Test Coverage**:
- Active JWT introspection with extension chain info
- Revoked JWT handling
- Malformed token handling
- Extension chain metadata inclusion

**Response Validation**:
```json
{
  "active": true,
  "sub": "testuser",
  "jwt_name": "TEST_TOKEN",
  "original_jwt_uuid": "12345678-1234-1234-1234-123456789012",
  "extension_count": 2,
  "supersedes": "87654321-4321-4321-4321-210987654321"
}
```

### 5. Database Schema Tests (`SchemaValidationTest`)

**Purpose**: Validate database schema structure and constraints.

**Validation Areas**:
- Table structure verification
- Column type and constraint validation
- Index existence and configuration
- Default value validation
- Primary key and foreign key relationships

**Schema Requirements**:
```sql
-- Required columns for extension chain
CREATE TABLE custom_jwt.jwt_metadata (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  jwt_uuid uuid NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  supersedes uuid,                    -- Previous version reference
  original_jwt_uuid uuid NOT NULL     -- First JWT in chain
);

-- Required indexes for performance
CREATE INDEX idx_custom_jwt_metadata_jwt_uuid ON custom_jwt.jwt_metadata (jwt_uuid, created_at DESC);
CREATE INDEX idx_custom_jwt_metadata_original ON custom_jwt.jwt_metadata (original_jwt_uuid);
```

## Running Tests

### Unit Tests Only
```bash
# Run entity tests
mvn test -Dtest=CustomJwtMetadataTest

# Run repository tests
mvn test -Dtest=CustomJwtMetadataRepoTest

# Run service tests
mvn test -Dtest=CustomJwtServiceTest
```

### Integration Tests
```bash
# Run introspection integration tests
mvn test -Dtest=TokenIntrospectionIntegrationTest

# Run database schema validation
mvn test -Dtest=SchemaValidationTest
```

### Full Test Suite
```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn test jacoco:report
```

## Test Data Management

### Test Profiles
- `application-test.properties` - Test-specific configuration
- In-memory H2 database for unit tests
- PostgreSQL testcontainers for integration tests

### Test Schema Setup
```sql
-- schema-test-setup.sql
CREATE SCHEMA IF NOT EXISTS custom_jwt;
CREATE SCHEMA IF NOT EXISTS auth;

-- Create tables with full schema
-- Create indexes
-- Create test data (if needed)
```

### Mock Data Patterns
```java
// UUID generation for predictable tests
UUID originalJwtUuid = UUID.fromString("12345678-1234-1234-1234-123456789012");
UUID extension1JwtUuid = UUID.fromString("87654321-4321-4321-4321-210987654321");

// Timestamp management
Instant baseTime = Instant.now();
Instant issuedAt = baseTime.minusSeconds(3600);
Instant expiresAt = baseTime.plusSeconds(3600);
```

## Test Assertions

### Extension Chain Validation
```java
// Verify chain structure
assertEquals(originalJwtUuid, extension.getOriginalJwtUuid());
assertEquals(previousVersionId, extension.getSupersedes());

// Verify chronological order
assertTrue(extension.getCreatedAt().isAfter(original.getCreatedAt()));

// Verify current version lookup
Optional<CustomJwtMetadata> current = repo.findCurrentVersionByJwtUuid(jwtUuid);
assertTrue(current.isPresent());
assertEquals(latestVersionId, current.get().getId());
```

### Database Constraint Validation
```java
// Verify required fields are not nullable
validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "original_jwt_uuid", false);

// Verify default values
validateColumnDefault(metaData, "custom_jwt", "jwt_metadata", "created_at", "now()");

// Verify index existence
assertTrue(indexExists(metaData, "custom_jwt", "jwt_metadata", "idx_custom_jwt_metadata_jwt_uuid"));
```

## Performance Testing

### Load Test Scenarios
1. **High-frequency JWT generation** - Test metadata insertion performance
2. **Extension chain queries** - Test query performance with long chains
3. **Bulk introspection** - Test concurrent introspection requests
4. **Database cleanup** - Test expired token cleanup performance

### Performance Benchmarks
```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testExtensionChainQueryPerformance() {
    // Create chain of 100 extensions
    // Verify current version lookup under 100ms
    // Verify full chain retrieval under 500ms
}
```

## Continuous Integration

### GitHub Actions Configuration
```yaml
- name: Run Unit Tests
  run: mvn test -Dtest="*Test"

- name: Run Integration Tests
  run: mvn test -Dtest="*IntegrationTest"

- name: Database Tests
  run: mvn test -Dtest="SchemaValidationTest"
```

### Test Coverage Requirements
- **Unit Tests**: >90% line coverage
- **Integration Tests**: Critical path coverage
- **Database Tests**: 100% schema validation

## Troubleshooting

### Common Test Issues

1. **Database Connection Errors**
   - Check test database configuration
   - Verify schema creation scripts
   - Ensure proper test isolation

2. **Extension Chain Test Failures**
   - Verify UUID generation consistency
   - Check timestamp ordering
   - Validate foreign key relationships

3. **Mock Configuration Issues**
   - Ensure proper mock setup in @BeforeEach
   - Verify argument matchers for complex objects
   - Check mock interaction verification

### Debug Tips
```java
// Enable SQL logging for repository tests
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true"
})

// Add detailed assertion messages
assertEquals(expected, actual,
    String.format("Extension chain validation failed for JWT %s", jwtUuid));
```

## Future Enhancements

### Planned Test Additions
1. **Performance benchmarks** for large extension chains
2. **Concurrent modification tests** for race conditions
3. **Migration tests** for schema updates
4. **Chaos engineering tests** for failure scenarios

### Test Automation
1. **Automated test generation** for new JWT scenarios
2. **Property-based testing** for edge cases
3. **Contract testing** for API compatibility
4. **Visual regression testing** for documentation