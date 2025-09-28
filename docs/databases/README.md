# TIM Database Documentation

## Overview

This directory contains comprehensive documentation for TIM's PostgreSQL database system, covering setup, architecture, table structures, operations, and maintenance procedures.

## Database Architecture

TIM uses a **dual-schema PostgreSQL architecture** to separate custom JWT functionality from TARA (Estonian e-ID) OAuth integration:

- **`custom` schema**: Custom JWT tokens for API authentication and service-to-service communication
- **`tara` schema**: TARA OAuth tokens for Estonian government identity authentication

Both schemas maintain similar table structures but serve different token ecosystems with independent security, compliance, and operational requirements.

## Documentation Structure

### [01. Database Setup](./01-database-setup.md)
**Complete database initialization and configuration guide**

- Docker Compose infrastructure setup
- PostgreSQL container configuration
- Automatic schema and table creation
- Connection parameters and security
- Development vs production considerations
- Backup and recovery procedures

**Key Topics:**
- Container orchestration with Docker
- Initialization script (`db/init.sql`) walkthrough
- Schema management approach (manual vs automated)
- Performance tuning for containerized deployment

### [02. Schema Architecture](./02-schema-architecture.md)
**Comprehensive schema design rationale and patterns**

- Why separate `custom` and `tara` schemas?
- Benefits of identical table structures across schemas
- Security isolation and compliance requirements
- Application integration patterns
- Schema evolution strategy

**Key Topics:**
- Functional separation between custom and government tokens
- Data segregation for security and compliance
- JPA entity mapping and repository patterns
- Performance implications of dual-schema design

### [03. Custom JWT Tables](./03-custom-tables.md)
**Detailed documentation of custom schema tables**

- `custom.jwt_metadata`: Token tracking and audit trails
- `custom.denylist`: Token revocation management
- `custom.allowlist`: Optional token allowlist (future use)
- Field-by-field explanations with usage examples
- Performance optimization and indexing strategies

**Key Topics:**
- JWT-to-database correlation mechanisms
- Claim tracking and analytics capabilities
- Revocation workflows and security procedures
- Maintenance and cleanup operations

### [04. TARA Tables](./04-tara-tables.md)
**TARA schema tables and government integration**

- Identical structure to custom tables but different purpose
- `tara.oauth_state`: OAuth PKCE flow management
- Government compliance and audit requirements
- Estonian e-ID integration patterns
- Extended retention policies for government tokens

**Key Topics:**
- TARA (Estonian e-ID) authentication system integration
- OAuth 2.0 and PKCE security flows
- Government compliance and data protection requirements
- Comparison with custom schema functionality

### [05. JWT-Database Correlation](./05-jwt-correlation.md)
**How JWT tokens link to database records**

- JWT ID (`jti`) as the primary correlation key
- Token lifecycle from generation to revocation
- Complete examples with real JWT tokens
- Cross-table relationships and data integrity
- Performance implications of correlation patterns

**Key Topics:**
- RFC 7519 JWT standard compliance
- UUID-based token identification
- Database lookup patterns for validation
- Audit trail construction using correlation

### [06. Database Operations](./06-operations.md)
**Comprehensive operational guide**

- Common database queries and operations
- Maintenance procedures and cleanup scripts
- Performance monitoring and optimization
- Emergency procedures and troubleshooting
- Backup and recovery workflows

**Key Topics:**
- Production maintenance schedules
- Security incident response procedures
- Performance tuning and index management
- Health monitoring and alerting

## Quick Reference

### Connection Information
```bash
# Docker container access
docker exec -it tim-postgres psql -U tim -d tim

# External connection (development)
Host: localhost:9876
Database: tim
Username: tim
Password: 123
```

### Database Structure
```
tim (database)
├── custom (schema)
│   ├── jwt_metadata    [60 tokens tracked]
│   ├── denylist        [0 revoked tokens]
│   └── allowlist       [future use]
└── tara (schema)
    ├── jwt_metadata    [government tokens]
    ├── denylist        [government revocations]
    ├── allowlist       [government allowlist]
    └── oauth_state     [OAuth PKCE flows]
```

### Key Tables and Relationships

**JWT Metadata Tables:**
- **Purpose**: Track all issued JWT tokens for auditing and analytics
- **Primary Key**: `jwt_uuid` (correlates to JWT's `jti` claim)
- **Fields**: `claim_keys`, `issued_at`, `expires_at`

**Denylist Tables:**
- **Purpose**: Manage revoked JWT tokens
- **Primary Key**: `jwt_uuid` (links to metadata)
- **Usage**: Fast revocation checks during validation

**OAuth State Table (TARA only):**
- **Purpose**: Manage OAuth PKCE flows for TARA authentication
- **Primary Key**: `state` (OAuth state parameter)
- **Security**: Prevents CSRF attacks in OAuth flows

## Common Operations

### Token Validation Query
```sql
-- Fast revocation check using primary key
SELECT jwt_uuid FROM custom.denylist
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';
```

### Analytics Query
```sql
-- Popular claim combinations
SELECT claim_keys, COUNT(*) as usage_count
FROM custom.jwt_metadata
GROUP BY claim_keys
ORDER BY COUNT(*) DESC;
```

### Cleanup Operations
```sql
-- Daily cleanup of expired entries
DELETE FROM custom.denylist WHERE expires_at < NOW();
DELETE FROM tara.oauth_state WHERE created_at < NOW() - INTERVAL '15 minutes';
```

## Security and Compliance

### Data Protection
- **JWT Content**: Only metadata stored, not actual JWT content
- **Personal Data**: No PII stored in database (only claim keys)
- **Retention**: Configurable retention periods per schema
- **Isolation**: Complete separation between custom and government data

### Compliance Features
- **Audit Trails**: Complete token lifecycle tracking
- **Government Standards**: TARA integration meets Estonian e-governance requirements
- **GDPR Compliance**: Data minimization and retention controls
- **Access Control**: Schema-level permissions and isolation

## Performance Characteristics

### Current Scale
- **Custom Tokens**: 60 tracked tokens
- **TARA Tokens**: Available for government integration
- **Revocations**: 0 current revoked tokens
- **Storage**: Minimal overhead with UUID-based indexing

### Optimization Features
- **Primary Key Indexes**: O(1) token lookup performance
- **Cleanup Indexes**: Efficient expiration-based cleanup
- **Connection Pooling**: HikariCP with 5-connection limit
- **Schema Separation**: Independent performance tuning per use case

## Development Workflow

### Local Development
1. Start PostgreSQL container: `docker-compose up -d postgres`
2. Verify initialization: `docker logs tim-postgres`
3. Connect and verify: `docker exec tim-postgres psql -U tim -d tim -c "\\dt"`
4. Run application: Tables auto-populated through JPA

### Schema Changes
1. Update `db/init.sql` with new DDL
2. Recreate containers: `docker-compose down -v && docker-compose up -d`
3. Test application integration
4. Document changes in relevant documentation files

### Testing Database Operations
```bash
# Generate test token
curl -X POST http://localhost:8085/jwt/custom/generate \
  -d '{"content": {"sub": "test"}, "expirationInMinutes": 60}'

# Verify database record
docker exec tim-postgres psql -U tim -d tim \
  -c "SELECT COUNT(*) FROM custom.jwt_metadata;"
```

## Troubleshooting

### Common Issues
- **Connection Refused**: Check container status and port mapping
- **Schema Not Found**: Verify initialization script execution
- **Slow Queries**: Check index usage and table statistics
- **High Storage**: Run cleanup procedures and vacuum operations

### Debug Commands
```bash
# Check container status
docker ps | grep tim-postgres

# View initialization logs
docker logs tim-postgres | grep -i error

# Verify schema structure
docker exec tim-postgres psql -U tim -d tim -c "\\dn"
```

## Future Enhancements

### Planned Improvements
- **Migration Framework**: Liquibase or Flyway integration
- **Additional Indexes**: Time-based and claim-based search optimization
- **Partitioning**: Table partitioning for large-scale deployments
- **Monitoring**: Enhanced metrics and alerting integration

### TARA Integration
- **Full OAuth Implementation**: Complete TARA authentication flows
- **Government Compliance**: Enhanced audit and retention features
- **Performance Optimization**: TARA-specific query patterns

---

## Getting Started

1. **Read [Database Setup](./01-database-setup.md)** for initial configuration
2. **Understand [Schema Architecture](./02-schema-architecture.md)** for design rationale
3. **Explore [Custom Tables](./03-custom-tables.md)** for table details
4. **Review [Operations Guide](./06-operations.md)** for maintenance procedures
5. **Study [JWT Correlation](./05-jwt-correlation.md)** for integration patterns

For specific use cases:
- **API Development**: Focus on Custom JWT Tables documentation
- **Government Integration**: Focus on TARA Tables documentation
- **DevOps/Maintenance**: Focus on Database Operations documentation
- **Security/Compliance**: Review all documents with emphasis on correlation and operations