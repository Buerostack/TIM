# Schema Isolation Security

## Overview

TIM implements strict schema isolation to prevent cross-contamination between custom JWT and OAuth2/auth functionalities. This ensures that:

- `/jwt/custom/*` endpoints cannot access `auth` schema
- `/auth/*` endpoints cannot access `custom_jwt` schema
- Only introspection service can access both (by design)

## Security Layers

### 1. Database User Separation (Primary Defense)

**Separate database users with schema-specific permissions:**

```sql
-- Custom JWT user: only custom_jwt schema access
CREATE USER tim_custom_jwt WITH PASSWORD 'custom_jwt_secure_pass';
GRANT USAGE ON SCHEMA custom_jwt TO tim_custom_jwt;
REVOKE ALL ON SCHEMA auth FROM tim_custom_jwt;

-- Auth user: only auth schema access
CREATE USER tim_auth WITH PASSWORD 'auth_secure_pass';
GRANT USAGE ON SCHEMA auth TO tim_auth;
REVOKE ALL ON SCHEMA custom_jwt FROM tim_auth;
```

**Benefits:**
- ✅ **Database-level enforcement** - impossible to bypass
- ✅ **PostgreSQL native security** - battle-tested
- ✅ **Clear audit trail** - database logs show which user accessed what

### 2. Spring Boot Multi-DataSource Configuration

**Separate connection pools and entity managers:**

```java
@EnableJpaRepositories(
    basePackages = "buerostack.jwt.repo",
    entityManagerFactoryRef = "customJwtEntityManagerFactory"
)

@EnableJpaRepositories(
    basePackages = "buerostack.auth.repo",
    entityManagerFactoryRef = "authEntityManagerFactory"
)
```

**Benefits:**
- ✅ **Physical separation** - different connections
- ✅ **Schema-specific defaults** - hibernate.default_schema
- ✅ **Package isolation** - repository packages can't cross-access

### 3. AOP Security Monitoring

**Runtime monitoring and logging:**

```java
@Around("execution(* buerostack.jwt.service..*(..))")
public Object enforceCustomJwtIsolation(ProceedingJoinPoint joinPoint) {
    // Log access for security auditing
    // Optional: runtime validation
}
```

**Benefits:**
- ✅ **Real-time monitoring** - detect isolation violations
- ✅ **Security auditing** - comprehensive access logs
- ✅ **Development aid** - catch violations early

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   JWT Module    │    │ Introspection   │    │   Auth Module   │
│                 │    │     Service     │    │                 │
│ - Generate      │    │                 │    │ - OAuth2 Login  │
│ - Extend        │    │ - Validate Both │    │ - Token Exchange│
│ - Revoke        │    │ - Smart Routing │    │ - Logout        │
│ - List          │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  tim_custom_jwt │    │   tim (admin)   │    │    tim_auth     │
│     User        │    │      User       │    │      User       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  custom_jwt     │    │   Both Schemas  │    │      auth       │
│    Schema       │    │   (Read Only)   │    │     Schema      │
│                 │    │                 │    │                 │
│ ✅ jwt_metadata │    │ ✅ custom_jwt.* │    │ ✅ jwt_metadata │
│ ✅ denylist     │    │ ✅ auth.*       │    │ ✅ denylist     │
│ ❌ auth.*       │    │                 │    │ ✅ oauth_state  │
│                 │    │                 │    │ ❌ custom_jwt.* │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Implementation Steps

### 1. Create Database Users

```bash
# Execute schema security setup
docker exec tim-postgres psql -U tim -d tim -f /path/to/schema-security.sql
```

### 2. Update Configuration

```properties
# Use secure configuration profile
spring.profiles.active=secure

# Or directly configure datasources
spring.datasource.custom-jwt.username=tim_custom_jwt
spring.datasource.auth.username=tim_auth
```

### 3. Deploy with Isolation

```bash
# Restart with secure configuration
docker-compose down
docker-compose up -d
```

## Testing Schema Isolation

### 1. Database Level Test

```sql
-- Test custom JWT user isolation
\c tim tim_custom_jwt
SELECT * FROM custom_jwt.jwt_metadata; -- ✅ Should work
SELECT * FROM auth.jwt_metadata;       -- ❌ Should fail

-- Test auth user isolation
\c tim tim_auth
SELECT * FROM auth.jwt_metadata;       -- ✅ Should work
SELECT * FROM custom_jwt.jwt_metadata; -- ❌ Should fail
```

### 2. Application Level Test

```bash
# Test JWT endpoints can't access auth data
curl -X POST /jwt/custom/generate -d '{...}'

# Test auth endpoints can't access custom JWT data
curl -X POST /auth/login -d '{...}'

# Test introspection works for both
curl -X POST /introspect -d 'token=...'
```

## Security Benefits

### Defense in Depth
1. **Database permissions** - Primary enforcement
2. **Connection separation** - Physical isolation
3. **Application monitoring** - Runtime detection
4. **Code organization** - Package boundaries

### Compliance
- ✅ **Principle of Least Privilege** - Each service has minimal access
- ✅ **Data Segregation** - Clear separation of concerns
- ✅ **Audit Trail** - All access is logged and traceable
- ✅ **Zero Trust** - No implicit trust between modules

### Operational Security
- **Intrusion Limitation** - Compromise of one module doesn't affect the other
- **Clear Boundaries** - Obvious security perimeter
- **Monitoring Points** - Well-defined access patterns
- **Incident Response** - Easy to trace data access

## Exception: Introspection Service

The introspection service (`/introspect`) **intentionally** has access to both schemas because:

1. **RFC 7662 Compliance** - Single endpoint for all token types
2. **Smart Routing** - Uses `token_type` claim to route correctly
3. **Read-Only Access** - Only validates, never modifies
4. **Controlled Access** - Uses primary admin connection with limited permissions

This is secure because:
- ✅ **Read-only operations** - No data modification
- ✅ **Token-type routing** - No blind schema access
- ✅ **Logging and monitoring** - All access is audited
- ✅ **Minimal surface** - Single, well-defined service

## Production Considerations

### Password Management
- Use strong, unique passwords for each database user
- Consider password rotation policies
- Store credentials securely (Kubernetes secrets, Vault, etc.)

### Monitoring
- Monitor database connections by user
- Alert on unusual cross-schema access attempts
- Log all introspection service operations

### Backup and Recovery
- Ensure backup includes all schemas
- Test restore procedures for schema isolation
- Verify permissions after restore operations

## Troubleshooting

### Common Issues

1. **Permission Denied Errors**
   - Check database user has correct schema permissions
   - Verify connection string uses correct username

2. **Repository Not Found**
   - Ensure repository packages are correctly configured
   - Check entity manager factory bean names

3. **Transaction Isolation**
   - Verify transaction managers are schema-specific
   - Check for cross-schema transaction boundaries